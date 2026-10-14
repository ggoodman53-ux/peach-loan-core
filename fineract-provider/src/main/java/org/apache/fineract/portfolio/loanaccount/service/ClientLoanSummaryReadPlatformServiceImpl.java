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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.data.ClientLoanSummaryData;
import org.apache.fineract.portfolio.loanaccount.data.LoanStatusEnumData;
import org.apache.fineract.portfolio.loanaccount.domain.LoanStatus;
import org.apache.fineract.portfolio.loanproduct.service.LoanEnumerations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@RequiredArgsConstructor
public class ClientLoanSummaryReadPlatformServiceImpl implements ClientLoanSummaryReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final ClientReadPlatformService clientReadPlatformService;

    @Override
    public List<ClientLoanSummaryData> retrieveActiveLoanSummaries(final Long clientId) {
        // Check if client exists
        this.clientReadPlatformService.retrieveOne(clientId);

        final ClientLoanSummaryMapper rm = new ClientLoanSummaryMapper();
        final String sql = "select " + rm.schema() + " where l.client_id = ? and l.loan_status_id = ? order by l.id";
        return this.jdbcTemplate.query(sql, rm, clientId, LoanStatus.ACTIVE.getValue()); // NOSONAR
    }

    private static final class ClientLoanSummaryMapper implements RowMapper<ClientLoanSummaryData> {

        public String schema() {
            return " l.id as id, lp.name as productName, l.principal_amount as principal, l.total_outstanding_derived as outstandingBalance, l.loan_status_id as statusId, (select min(ls.duedate) from m_loan_repayment_schedule ls where ls.loan_id = l.id and ls.completed_derived = false) as nextInstallmentDueDate from m_loan l join m_product_loan lp on lp.id = l.product_id ";
        }

        @Override
        public ClientLoanSummaryData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long id = JdbcSupport.getLong(rs, "id");
            final String productName = rs.getString("productName");
            final BigDecimal principal = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principal");
            final BigDecimal outstandingBalance = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "outstandingBalance");
            final LocalDate nextInstallmentDueDate = JdbcSupport.getLocalDate(rs, "nextInstallmentDueDate");
            final LoanStatusEnumData status = LoanEnumerations.status(JdbcSupport.getInteger(rs, "statusId"));

            return new ClientLoanSummaryData(id, productName, principal, outstandingBalance, nextInstallmentDueDate, status);
        }
    }
}
