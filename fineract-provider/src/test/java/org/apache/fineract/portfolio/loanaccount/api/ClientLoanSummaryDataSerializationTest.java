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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.fineract.infrastructure.core.serialization.ExcludeNothingWithPrettyPrintingOffJsonSerializerGoogleGson;
import org.apache.fineract.portfolio.loanaccount.data.ClientLoanSummaryData;
import org.apache.fineract.portfolio.loanproduct.service.LoanEnumerations;
import org.junit.jupiter.api.Test;

class ClientLoanSummaryDataSerializationTest {

    private final ExcludeNothingWithPrettyPrintingOffJsonSerializerGoogleGson serializer = new ExcludeNothingWithPrettyPrintingOffJsonSerializerGoogleGson();

    @Test
    void serialize_omitsNullNextInstallmentDueDate() {
        final JsonObject loan = serialize(createSummary(null));

        assertTrue(loan.has("id"));
        assertTrue(loan.has("productName"));
        assertTrue(loan.has("principal"));
        assertTrue(loan.has("outstandingBalance"));
        assertTrue(loan.has("status"));
        assertFalse(loan.has("nextInstallmentDueDate"));
    }

    @Test
    void serialize_includesNextInstallmentDueDateWhenPresent() {
        final JsonObject loan = serialize(createSummary(LocalDate.of(2025, 4, 15)));

        assertTrue(loan.has("nextInstallmentDueDate"));
        assertEquals(JsonParser.parseString("[2025,4,15]"), loan.get("nextInstallmentDueDate"));
    }

    private JsonObject serialize(final ClientLoanSummaryData summary) {
        final String json = this.serializer.serialize(List.of(summary));
        final JsonArray loans = JsonParser.parseString(json).getAsJsonArray();
        return loans.get(0).getAsJsonObject();
    }

    private static ClientLoanSummaryData createSummary(final LocalDate nextInstallmentDueDate) {
        return new ClientLoanSummaryData(82L, "Growth Loan", new BigDecimal("1250.00"), new BigDecimal("875.50"), nextInstallmentDueDate,
                LoanEnumerations.status(300));
    }
}
