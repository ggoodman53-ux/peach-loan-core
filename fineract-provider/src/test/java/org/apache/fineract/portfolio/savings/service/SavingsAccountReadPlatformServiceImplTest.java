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
package org.apache.fineract.portfolio.savings.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountAssembler;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransactionRepository;
import org.apache.fineract.useradministration.domain.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SavingsAccountReadPlatformServiceImplTest {

    @Mock
    private PlatformSecurityContext context;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private SavingsAccountAssembler savingsAccountAssembler;
    @Mock
    private PaginationHelper paginationHelper;
    @Mock
    private ColumnValidator columnValidator;
    @Mock
    private DatabaseSpecificSQLGenerator sqlGenerator;
    @Mock
    private SavingsAccountRepositoryWrapper savingsAccountRepositoryWrapper;
    @Mock
    private SavingsAccountTransactionRepository savingsAccountTransactionRepository;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AppUser appUser;

    private SavingsAccountReadPlatformServiceImpl service;

    @BeforeEach
    void setUp() {
        when(context.authenticatedUser()).thenReturn(appUser);
        when(appUser.getOffice().getHierarchy()).thenReturn(".");
        when(sqlGenerator.calcFoundRows()).thenReturn("");
        service = new SavingsAccountReadPlatformServiceImpl(context, jdbcTemplate, savingsAccountAssembler, paginationHelper,
                columnValidator, sqlGenerator, savingsAccountRepositoryWrapper, savingsAccountTransactionRepository);
    }

    @Test
    void retrieveAllMapsAllowedOrderByToFixedColumnAndNormalisesSortOrder() {
        service.retrieveAll(SearchParameters.builder().orderBy("accountNo").sortOrder("desc").build());

        String sql = captureExecutedSql();
        assertTrue(sql.endsWith(" order by sa.account_no DESC"), sql);
    }

    @Test
    void retrieveAllAcceptsQualifiedColumnName() {
        service.retrieveAll(SearchParameters.builder().orderBy("sa.id").sortOrder("ASC").build());

        String sql = captureExecutedSql();
        assertTrue(sql.endsWith(" order by sa.id ASC"), sql);
    }

    @Test
    void retrieveAllRejectsSortOrderThatIsNotAscOrDesc() {
        SearchParameters searchParameters = SearchParameters.builder().orderBy("sa.id")
                .sortOrder("ASC, sign(ascii(substr((select password from m_appuser limit 1),1,1))-50)").build();

        assertThrows(PlatformApiDataValidationException.class, () -> service.retrieveAll(searchParameters));
        verify(paginationHelper, never()).fetchPage(any(), anyString(), any(Object[].class), any(RowMapper.class));
    }

    @Test
    void retrieveAllRejectsOrderByThatIsNotAllowlisted() {
        SearchParameters searchParameters = SearchParameters.builder()
                .orderBy("sign(ascii(substr((select password from m_appuser limit 1),1,1))-50)").build();

        assertThrows(PlatformApiDataValidationException.class, () -> service.retrieveAll(searchParameters));
        verify(paginationHelper, never()).fetchPage(any(), anyString(), any(Object[].class), any(RowMapper.class));
    }

    @SuppressWarnings("unchecked")
    private String captureExecutedSql() {
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(paginationHelper).fetchPage(any(), sqlCaptor.capture(), any(Object[].class), any(RowMapper.class));
        return sqlCaptor.getValue();
    }
}
