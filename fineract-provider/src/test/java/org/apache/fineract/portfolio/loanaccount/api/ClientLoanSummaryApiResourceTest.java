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
package org.apache.fineract.portfolio.loanaccount.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.exception.NoAuthorizationException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.loanaccount.data.ClientLoanSummaryData;
import org.apache.fineract.portfolio.loanaccount.service.ClientLoanSummaryReadPlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientLoanSummaryApiResourceTest {

    private static final Long CLIENT_ID = 37L;

    @Mock
    private PlatformSecurityContext context;
    @Mock
    private ClientLoanSummaryReadPlatformService clientLoanSummaryReadPlatformService;
    @Mock
    private DefaultToApiJsonSerializer<ClientLoanSummaryData> toApiJsonSerializer;
    @Mock
    private AppUser appUser;

    private ClientLoanSummaryApiResource underTest;

    @BeforeEach
    void setUp() {
        when(context.authenticatedUser()).thenReturn(appUser);
        underTest = new ClientLoanSummaryApiResource(context, clientLoanSummaryReadPlatformService, toApiJsonSerializer);
    }

    @Test
    void retrieveClientLoanSummary_checksPermissionDelegatesAndReturnsSerializedOutput() {
        List<ClientLoanSummaryData> summaries = List.of();
        when(clientLoanSummaryReadPlatformService.retrieveActiveLoanSummaries(CLIENT_ID)).thenReturn(summaries);
        when(toApiJsonSerializer.serialize(summaries)).thenReturn("[]");

        String result = underTest.retrieveClientLoanSummary(CLIENT_ID);

        assertEquals("[]", result);
        verify(appUser).validateHasReadPermission("LOAN");
        verify(clientLoanSummaryReadPlatformService).retrieveActiveLoanSummaries(CLIENT_ID);
        verify(toApiJsonSerializer).serialize(summaries);
    }

    @Test
    void retrieveClientLoanSummary_permissionDenied_doesNotDelegate() {
        doThrow(new NoAuthorizationException("Permission denied")).when(appUser).validateHasReadPermission("LOAN");

        assertThrows(NoAuthorizationException.class, () -> underTest.retrieveClientLoanSummary(CLIENT_ID));

        verifyNoInteractions(clientLoanSummaryReadPlatformService);
    }
}
