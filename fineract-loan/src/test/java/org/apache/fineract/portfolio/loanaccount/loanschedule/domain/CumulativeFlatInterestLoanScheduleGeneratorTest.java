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
import static org.apache.fineract.portfolio.loanproduct.domain.InterestMethod.FLAT;
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

class CumulativeFlatInterestLoanScheduleGeneratorTest {

    private static final MockedStatic<MoneyHelper> MONEY_HELPER = mockStatic(MoneyHelper.class);
    private static final MathContext MATH_CONTEXT = new MathContext(19, RoundingMode.HALF_EVEN);
    private static final CurrencyData USD = new CurrencyData("USD", "US Dollar", 2, 0, "$", "currency.USD");
    private static final int NUMBER_OF_REPAYMENTS = 3;

    private final PaymentPeriodsInOneYearCalculator periodsCalculator = new DefaultPaymentPeriodsInOneYearCalculator();
    private final CumulativeFlatInterestLoanScheduleGenerator generator = new CumulativeFlatInterestLoanScheduleGenerator(
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
     * 1,000.00 at 10% flat for 3 months: total interest = 1,000 x 10% / 12 x 3 = 25.00. Neither 1,000 / 3 nor 25 / 3 is
     * representable in cents, so periods 1-2 get the rounded share (333.33 principal, 8.33 interest) and the last
     * period must absorb the remainder (333.34 / 8.34) so the schedule repays exactly the principal and the quoted
     * interest.
     */
    @Test
    void testFlatEqualInstallments_ChargesSameInterestEachPeriodAndLastPeriodAbsorbsRounding() {
        Money principal = Money.of(USD, BigDecimal.valueOf(1000));
        LoanApplicationTerms terms = flatTerms(principal, BigDecimal.TEN);
        Money totalInterestDue = terms.calculateTotalInterestCharged(periodsCalculator, MATH_CONTEXT);
        terms.updateTotalInterestDue(totalInterestDue);

        assertMoney("25.00", totalInterestDue);

        Money cumulativePrincipal = Money.zero(USD);
        Money cumulativeInterest = Money.zero(USD);
        String[] expectedPrincipal = { "333.33", "333.33", "333.34" };
        String[] expectedInterest = { "8.33", "8.33", "8.34" };
        LocalDate periodStart = LocalDate.of(2024, 1, 1);

        for (int period = 1; period <= NUMBER_OF_REPAYMENTS; period++) {
            LocalDate periodEnd = periodStart.plusMonths(1);
            PrincipalInterest result = generator.calculatePrincipalInterestComponentsForPeriod(periodsCalculator, ZERO, cumulativePrincipal,
                    cumulativeInterest, totalInterestDue, Money.zero(USD), principal.minus(cumulativePrincipal), terms, period,
                    MATH_CONTEXT, new TreeMap<>(), new HashMap<>(), periodStart, periodEnd, emptyList());

            assertMoney(expectedPrincipal[period - 1], result.principal(), "principal for period " + period);
            assertMoney(expectedInterest[period - 1], result.interest(), "interest for period " + period);

            cumulativePrincipal = cumulativePrincipal.plus(result.principal());
            cumulativeInterest = cumulativeInterest.plus(result.interest());
            periodStart = periodEnd;
        }

        assertMoney("1000.00", cumulativePrincipal, "schedule must repay exactly the disbursed principal");
        assertMoney("25.00", cumulativeInterest, "schedule must charge exactly the total flat interest");
    }

    private static void assertMoney(String expected, Money actual) {
        assertMoney(expected, actual, null);
    }

    private static void assertMoney(String expected, Money actual, String message) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.getAmount()),
                (message == null ? "" : message + ": ") + "expected " + expected + " but was " + actual.getAmount());
    }

    private static LoanApplicationTerms flatTerms(Money principal, BigDecimal annualInterestRate) {
        LocalDate disbursementDate = LocalDate.of(2024, 1, 1);
        return LoanApplicationTerms.assembleFrom(USD, NUMBER_OF_REPAYMENTS, MONTHS, NUMBER_OF_REPAYMENTS, 1, MONTHS, null, INVALID,
                EQUAL_INSTALLMENTS, FLAT, annualInterestRate, YEARS, annualInterestRate, SAME_AS_REPAYMENT_PERIOD, false, principal,
                disbursementDate, null, disbursementDate.plusMonths(1), null, null, null, null, null, Money.zero(USD), false, null,
                emptyList(), null, null, DaysInMonthType.ACTUAL, DaysInYearType.ACTUAL, false, null, null, null, null, null, ZERO, null,
                NONE, null, principal.getAmount(), emptyList(), true, 0, false, null, false, false, false, null, false, false, null, false,
                DISBURSEMENT_DATE, disbursementDate, CUMULATIVE, LoanScheduleProcessingType.HORIZONTAL, null, false, null, null, false,
                null, false, null, null, null, false, null, null, null, false, false);
    }
}
