/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.loanaccount.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.gson.JsonElement;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.portfolio.charge.domain.ChargeRepositoryWrapper;
import org.apache.fineract.portfolio.charge.exception.LoanChargeNotFoundException;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargeRepository;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanChargeAssemblerTest {

    private static final Long PRODUCT_ID = 1L;
    private static final Long LOAN_ID = 10L;
    private static final Long OTHER_LOAN_ID = 20L;
    private static final Long LOAN_CHARGE_ID = 100L;

    private final FromJsonHelper fromJsonHelper = new FromJsonHelper();

    @Mock
    private ChargeRepositoryWrapper chargeRepository;
    @Mock
    private LoanChargeRepository loanChargeRepository;
    @Mock
    private LoanProductRepository loanProductRepository;
    @Mock
    private ExternalIdFactory externalIdFactory;
    @Mock
    private LoanChargeService loanChargeService;

    private LoanChargeAssembler underTest;

    @BeforeEach
    void setUp() {
        underTest = new LoanChargeAssembler(fromJsonHelper, chargeRepository, loanChargeRepository, loanProductRepository,
                externalIdFactory, loanChargeService);
        final LoanProduct loanProduct = mock(LoanProduct.class);
        when(loanProductRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(loanProduct));
    }

    private JsonElement requestWithExistingCharge() {
        return fromJsonHelper.parse("{\"productId\": " + PRODUCT_ID + ", \"locale\": \"en\", \"dateFormat\": \"dd MMMM yyyy\","
                + " \"principal\": 1000, \"numberOfRepayments\": 4, \"charges\": [{\"id\": " + LOAN_CHARGE_ID + ", \"amount\": 1}]}");
    }

    private static Loan loanWithId(final Long id) {
        final Loan loan = mock(Loan.class);
        when(loan.getId()).thenReturn(id);
        return loan;
    }

    @Test
    void existingChargeIdIsRejectedWhenNoLoanIsBeingModified() {
        assertThrows(LoanChargeNotFoundException.class, () -> underTest.fromParsedJson(requestWithExistingCharge(), List.of(), null));

        verifyNoInteractions(loanChargeRepository);
        verifyNoInteractions(loanChargeService);
    }

    @Test
    void existingChargeIdOfAnotherLoanIsRejected() {
        final Loan targetLoan = loanWithId(LOAN_ID);
        final Loan otherLoan = loanWithId(OTHER_LOAN_ID);
        final LoanCharge foreignCharge = mock(LoanCharge.class);
        when(foreignCharge.getLoan()).thenReturn(otherLoan);
        when(loanChargeRepository.findById(LOAN_CHARGE_ID)).thenReturn(Optional.of(foreignCharge));

        assertThrows(LoanChargeNotFoundException.class, () -> underTest.fromParsedJson(requestWithExistingCharge(), List.of(), targetLoan));

        verify(loanChargeService, never()).update(any(), any(), any(), any());
    }

    @Test
    void existingChargeIdWithoutLoanIsRejected() {
        final Loan targetLoan = loanWithId(LOAN_ID);
        final LoanCharge orphanCharge = mock(LoanCharge.class);
        when(orphanCharge.getLoan()).thenReturn(null);
        when(loanChargeRepository.findById(LOAN_CHARGE_ID)).thenReturn(Optional.of(orphanCharge));

        assertThrows(LoanChargeNotFoundException.class, () -> underTest.fromParsedJson(requestWithExistingCharge(), List.of(), targetLoan));

        verify(loanChargeService, never()).update(any(), any(), any(), any());
    }

    @Test
    void existingChargeIdOfTheModifiedLoanIsUpdated() {
        final Loan targetLoan = loanWithId(LOAN_ID);
        final LoanCharge ownCharge = mock(LoanCharge.class);
        when(ownCharge.getLoan()).thenReturn(targetLoan);
        when(ownCharge.isTrancheDisbursementCharge()).thenReturn(false);
        when(loanChargeRepository.findById(LOAN_CHARGE_ID)).thenReturn(Optional.of(ownCharge));

        final Set<LoanCharge> result = underTest.fromParsedJson(requestWithExistingCharge(), List.of(), targetLoan);

        assertEquals(Set.of(ownCharge), result);
        verify(loanChargeService).update(ownCharge, BigDecimal.ONE, null, 4);
    }
}
