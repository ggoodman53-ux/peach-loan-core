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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.junit.jupiter.api.Test;

class DefaultPaymentPeriodsInOneYearCalculatorTest {

    private static final MathContext MATH_CONTEXT = new MathContext(19, RoundingMode.HALF_EVEN);
    private static final LocalDate PERIOD_START = LocalDate.of(2024, 1, 1);
    private static final LocalDate PERIOD_DUE = LocalDate.of(2024, 2, 1);

    private final DefaultPaymentPeriodsInOneYearCalculator calculator = new DefaultPaymentPeriodsInOneYearCalculator();

    /**
     * Periods-per-year is the divisor that turns an annual nominal rate into a per-installment rate for every
     * cumulative schedule, so a wrong value here mis-prices every installment.
     */
    @Test
    void testCalculate_ReturnsPeriodsPerYearForEachRepaymentFrequency() {
        assertEquals(365, calculator.calculate(PeriodFrequencyType.DAYS));
        assertEquals(52, calculator.calculate(PeriodFrequencyType.WEEKS));
        assertEquals(12, calculator.calculate(PeriodFrequencyType.MONTHS));
        assertEquals(1, calculator.calculate(PeriodFrequencyType.YEARS));
        assertEquals(0, calculator.calculate(PeriodFrequencyType.INVALID));
    }

    @Test
    void testInterestChargingGraceFraction_IsZeroWhenNoInterestChargedFromDate() {
        BigDecimal fraction = calculator.calculatePortionOfRepaymentPeriodInterestChargingGrace(PERIOD_START, PERIOD_DUE, null,
                PeriodFrequencyType.MONTHS, 1, MATH_CONTEXT);

        assertEquals(0, BigDecimal.ZERO.compareTo(fraction), "No interest-free days when interest is charged from disbursement");
    }

    @Test
    void testInterestChargingGraceFraction_IsWholePeriodWhenInterestStartsAfterPeriodEnds() {
        BigDecimal fraction = calculator.calculatePortionOfRepaymentPeriodInterestChargingGrace(PERIOD_START, PERIOD_DUE,
                LocalDate.of(2024, 2, 15), PeriodFrequencyType.MONTHS, 1, MATH_CONTEXT);

        assertEquals(0, BigDecimal.ONE.compareTo(fraction), "Entire period is interest free when interest starts after its due date");
    }

    @Test
    void testInterestChargingGraceFraction_IsZeroWhenInterestStartsOnPeriodStart() {
        BigDecimal fraction = calculator.calculatePortionOfRepaymentPeriodInterestChargingGrace(PERIOD_START, PERIOD_DUE, PERIOD_START,
                PeriodFrequencyType.MONTHS, 1, MATH_CONTEXT);

        assertEquals(0, BigDecimal.ZERO.compareTo(fraction), "No interest-free days when interest starts on the period start date");
    }

    /**
     * Monthly periods are treated as 30 days: interest charged from day 16 leaves 15 interest-free days = half a
     * period.
     */
    @Test
    void testInterestChargingGraceFraction_MonthlyPeriodUsesThirtyDayMonth() {
        BigDecimal fraction = calculator.calculatePortionOfRepaymentPeriodInterestChargingGrace(PERIOD_START, PERIOD_DUE,
                LocalDate.of(2024, 1, 16), PeriodFrequencyType.MONTHS, 1, MATH_CONTEXT);

        assertEquals(0, new BigDecimal("0.5").compareTo(fraction), "15 of 30 days should be interest free");
    }

    @Test
    void testInterestChargingGraceFraction_WeeklyPeriodUsesSevenDayWeek() {
        BigDecimal fraction = calculator.calculatePortionOfRepaymentPeriodInterestChargingGrace(PERIOD_START, LocalDate.of(2024, 1, 8),
                LocalDate.of(2024, 1, 4), PeriodFrequencyType.WEEKS, 1, MATH_CONTEXT);

        BigDecimal expected = BigDecimal.valueOf(3).divide(BigDecimal.valueOf(7), MATH_CONTEXT);
        assertEquals(0, expected.compareTo(fraction), "3 of 7 days should be interest free");
    }
}
