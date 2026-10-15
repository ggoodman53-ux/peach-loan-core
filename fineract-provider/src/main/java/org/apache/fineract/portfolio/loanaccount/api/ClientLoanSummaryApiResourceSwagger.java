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

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

final class ClientLoanSummaryApiResourceSwagger {

    private ClientLoanSummaryApiResourceSwagger() {}

    @Schema(description = "GetClientsClientIdLoanSummaryResponse")
    public static final class GetClientsClientIdLoanSummaryResponse {

        private GetClientsClientIdLoanSummaryResponse() {}

        static final class GetClientsClientIdLoanSummaryStatus {

            private GetClientsClientIdLoanSummaryStatus() {}

            @Schema(example = "300")
            public Long id;
            @Schema(example = "loanStatusType.active")
            public String code;
            @Schema(example = "Active")
            public String value;
            @Schema(example = "false")
            public Boolean pendingApproval;
            @Schema(example = "false")
            public Boolean waitingForDisbursal;
            @Schema(example = "true")
            public Boolean active;
            @Schema(example = "false")
            public Boolean closedObligationsMet;
            @Schema(example = "false")
            public Boolean closedWrittenOff;
            @Schema(example = "false")
            public Boolean closedRescheduled;
            @Schema(example = "false")
            public Boolean closed;
            @Schema(example = "false")
            public Boolean overpaid;
        }

        @Schema(example = "82", description = "Loan id")
        public Long id;
        @Schema(example = "Growth Loan", description = "Loan product name")
        public String productName;
        @Schema(example = "1250.00", description = "Loan principal amount")
        public BigDecimal principal;
        @Schema(example = "875.50", description = "Total amount outstanding on the loan")
        public BigDecimal outstandingBalance;
        @Schema(example = "[2025, 4, 15]", description = "Earliest due date among the loan's incomplete installments. Omitted from the response when no installment is outstanding.")
        public LocalDate nextInstallmentDueDate;
        public GetClientsClientIdLoanSummaryStatus status;
    }
}
