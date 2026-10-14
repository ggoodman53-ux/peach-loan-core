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
package org.apache.fineract.portfolio.loanaccount.loanschedule.domain;

import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptyList;
import static org.apache.fineract.portfolio.common.domain.DayOfWeekType.INVALID;
import static org.apache.fineract.portfolio.common.domain.PeriodFrequencyType.MONTHS;
import static org.apache.fineract.portfolio.common.domain.PeriodFrequencyType.YEARS;
import static org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleType.CUMULATIVE;
import static org.apache.fineract.portfolio.loanproduct.domain.AmortizationMethod.EQUAL_INSTALLMENTS;
import static org.apache.fineract.portfolio.loanproduct.domain.InterestCalculationPeriodMethod.SAME_AS_REPAYMENT_PERIOD;
import static org.apache.fineract.portfolio.loanproduct.domain.InterestMethod.DECLINING_BALANCE;
import static org.apache.fineract.portfolio.loanproduct.domain.LoanPreCloseInterestCalculationStrategy.NONE;
import static org.apache.fineract.portfolio.loanproduct.domain.RepaymentStartDateType.DISBURSEMENT_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.TreeMap;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.organisation.monetary.mapper.CurrencyMapper;
import org.apache.fineract.portfolio.common.domain.DaysInMonthType;
import org.apache.fineract.portfolio.common.domain.DaysInYearType;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class CumulativeDecliningBalanceInterestLoanScheduleGeneratorTest {

    private static final MockedStatic<MoneyHelper> MONEY_HELPER = mockStatic(MoneyHelper.class);
    private static final MathContext MATH_CONTEXT = new MathContext(19, RoundingMode.HALF_EVEN);
    private static final CurrencyData USD = new CurrencyData("USD", "US Dollar", 2, 0, "$", "currency.USD");
    private static final int NUMBER_OF_REPAYMENTS = 4;

    private final PaymentPeriodsInOneYearCalculator periodsCalculator = new DefaultPaymentPeriodsInOneYearCalculator();
    private final CumulativeDecliningBalanceInterestLoanScheduleGenerator generator = new CumulativeDecliningBalanceInterestLoanScheduleGenerator(
            new DefaultScheduledDateGenerator(), periodsCalculator, mock(LoanTransactionRepository.class), mock(CurrencyMapper.class));

    @BeforeAll
    static void init() {
        MONEY_HELPER.when(MoneyHelper::getRoundingMode).thenReturn(RoundingMode.HALF_EVEN);
        MONEY_HELPER.when(MoneyHelper::getMathContext).thenReturn(MATH_CONTEXT);
    }

    @AfterAll
    static void tearDown() {
        MONEY_HELPER.close();
    }

    /**
     * 1,000.00 at 12% p.a. declining balance, 4 equal monthly installments (EMI = PMT(1%, 4, 1000) = 256.28). Interest
     * must be 1% of the balance still outstanding at the start of each period, so it shrinks every month while the
     * principal share grows. The last period's principal is trued up (253.74 -> 253.75) so the balance reaches zero.
     */
    @Test
    void testDecliningBalanceEqualInstallments_InterestIsChargedOnOutstandingBalance() {
        Money principal = Money.of(USD, BigDecimal.valueOf(1000));
        LoanApplicationTerms terms = decliningBalanceTerms(principal, BigDecimal.valueOf(12));

        String[] expectedInterest = { "10.00", "7.54", "5.05", "2.54" };
        String[] expectedPrincipal = { "246.28", "248.74", "251.23", "253.75" };
        Money cumulativePrincipal = Money.zero(USD);
        LocalDate periodStart = LocalDate.of(2024, 1, 1);

        for (int period = 1; period <= NUMBER_OF_REPAYMENTS; period++) {
            LocalDate periodEnd = periodStart.plusMonths(1);
            Money outstandingBalance = principal.minus(cumulativePrincipal);
            PrincipalInterest result = generator.calculatePrincipalInterestComponentsForPeriod(periodsCalculator, ZERO, cumulativePrincipal,
                    Money.zero(USD), Money.zero(USD), Money.zero(USD), outstandingBalance, terms, period, MATH_CONTEXT, new TreeMap<>(),
                    new HashMap<>(), periodStart, periodEnd, emptyList());

            assertMoney(expectedInterest[period - 1], result.interest(), "interest for period " + period);
            assertMoney(expectedPrincipal[period - 1], result.principal(), "principal for period " + period);
            if (period < NUMBER_OF_REPAYMENTS) {
                assertMoney("256.28", result.principal().plus(result.interest()), "installment for period " + period);
            }

            cumulativePrincipal = cumulativePrincipal.plus(result.principal());
            periodStart = periodEnd;
        }

        assertMoney("1000.00", cumulativePrincipal, "schedule must repay exactly the disbursed principal");
    }

    private static void assertMoney(String expected, Money actual, String message) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.getAmount()),
                message + ": expected " + expected + " but was " + actual.getAmount());
    }

    private static LoanApplicationTerms decliningBalanceTerms(Money principal, BigDecimal annualInterestRate) {
        LocalDate disbursementDate = LocalDate.of(2024, 1, 1);
        return LoanApplicationTerms.assembleFrom(USD, NUMBER_OF_REPAYMENTS, MONTHS, NUMBER_OF_REPAYMENTS, 1, MONTHS, null, INVALID,
                EQUAL_INSTALLMENTS, DECLINING_BALANCE, annualInterestRate, YEARS, annualInterestRate, SAME_AS_REPAYMENT_PERIOD, false,
                principal, disbursementDate, null, disbursementDate.plusMonths(1), null, null, null, null, null, Money.zero(USD), false,
                null, emptyList(), null, null, DaysInMonthType.ACTUAL, DaysInYearType.ACTUAL, false, null, null, null, null, null, ZERO,
                null, NONE, null, principal.getAmount(), emptyList(), true, 0, false, null, false, false, false, null, false, false, null,
                false, DISBURSEMENT_DATE, disbursementDate, CUMULATIVE, LoanScheduleProcessingType.HORIZONTAL, null, false, null, null,
                false, null, false, null, null, null, false, null, null, null, false, false);
    }
}
