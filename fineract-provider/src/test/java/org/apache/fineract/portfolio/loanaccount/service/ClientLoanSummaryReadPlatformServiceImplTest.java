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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import org.apache.fineract.portfolio.client.exception.ClientNotFoundException;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.data.ClientLoanSummaryData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class ClientLoanSummaryReadPlatformServiceImplTest {

    private static final Long CLIENT_ID = 37L;

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private ClientReadPlatformService clientReadPlatformService;

    @InjectMocks
    private ClientLoanSummaryReadPlatformServiceImpl underTest;

    @Test
    void returnsActiveLoansForClient() {
        List<ClientLoanSummaryData> expected = List.of();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(CLIENT_ID), eq(300))).thenReturn(expected);

        List<ClientLoanSummaryData> actual = underTest.retrieveActiveLoanSummaries(CLIENT_ID);

        assertSame(expected, actual);
        verify(clientReadPlatformService).retrieveOne(CLIENT_ID);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), eq(CLIENT_ID), eq(300));
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("l.client_id = ?"));
        assertTrue(sql.contains("l.loan_status_id = ?"));
        assertTrue(sql.contains("ls.completed_derived = false"));
        assertTrue(sql.contains("from m_loan l"));
    }

    @Test
    void unknownOrOutOfScopeClient_throwsAndDoesNotQuery() {
        when(clientReadPlatformService.retrieveOne(CLIENT_ID)).thenThrow(new ClientNotFoundException(CLIENT_ID));

        assertThrows(ClientNotFoundException.class, () -> underTest.retrieveActiveLoanSummaries(CLIENT_ID));

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void rowMapper_mapsAllFields() throws Exception {
        RowMapper<ClientLoanSummaryData> rowMapper = captureRowMapper();
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.findColumn("id")).thenReturn(1);
        when(resultSet.findColumn("statusId")).thenReturn(5);
        when(resultSet.getLong(1)).thenReturn(82L);
        when(resultSet.getInt(5)).thenReturn(300);
        when(resultSet.wasNull()).thenReturn(false);
        when(resultSet.getString("productName")).thenReturn("Growth Loan");
        when(resultSet.getBigDecimal("principal")).thenReturn(new BigDecimal("1250.00"));
        when(resultSet.getBigDecimal("outstandingBalance")).thenReturn(new BigDecimal("875.50"));
        when(resultSet.getDate("nextInstallmentDueDate")).thenReturn(Date.valueOf(LocalDate.of(2025, 4, 15)));

        ClientLoanSummaryData data = rowMapper.mapRow(resultSet, 0);

        assertEquals(82L, data.getId());
        assertEquals("Growth Loan", data.getProductName());
        assertEquals(new BigDecimal("1250.00"), data.getPrincipal());
        assertEquals(new BigDecimal("875.50"), data.getOutstandingBalance());
        assertEquals(LocalDate.of(2025, 4, 15), data.getNextInstallmentDueDate());
        assertEquals(300L, data.getStatus().getId());
        assertTrue(data.getStatus().isActive());
    }

    @Test
    void rowMapper_noOutstandingInstallment_nextDueDateIsNull() throws Exception {
        RowMapper<ClientLoanSummaryData> rowMapper = captureRowMapper();
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.findColumn("id")).thenReturn(1);
        when(resultSet.findColumn("statusId")).thenReturn(5);
        when(resultSet.getLong(1)).thenReturn(82L);
        when(resultSet.getInt(5)).thenReturn(300);
        when(resultSet.wasNull()).thenReturn(false);
        when(resultSet.getString("productName")).thenReturn("Growth Loan");
        when(resultSet.getBigDecimal("principal")).thenReturn(null);
        when(resultSet.getBigDecimal("outstandingBalance")).thenReturn(null);
        when(resultSet.getDate("nextInstallmentDueDate")).thenReturn(null);

        ClientLoanSummaryData data = rowMapper.mapRow(resultSet, 0);

        assertNull(data.getNextInstallmentDueDate());
        assertEquals(BigDecimal.ZERO, data.getPrincipal());
        assertEquals(BigDecimal.ZERO, data.getOutstandingBalance());
    }

    @SuppressWarnings("unchecked")
    private RowMapper<ClientLoanSummaryData> captureRowMapper() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(CLIENT_ID), eq(300))).thenReturn(List.of());
        underTest.retrieveActiveLoanSummaries(CLIENT_ID);

        ArgumentCaptor<RowMapper<ClientLoanSummaryData>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        verify(jdbcTemplate).query(anyString(), mapperCaptor.capture(), eq(CLIENT_ID), eq(300));
        return mapperCaptor.getValue();
    }
}
