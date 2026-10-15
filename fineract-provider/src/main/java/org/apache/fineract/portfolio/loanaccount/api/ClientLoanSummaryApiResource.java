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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.loanaccount.data.ClientLoanSummaryData;
import org.apache.fineract.portfolio.loanaccount.service.ClientLoanSummaryReadPlatformService;
import org.springframework.stereotype.Component;

@Path("/v1/clients/{clientId}/loansummary")
@Component
@Tag(name = "Client Loan Summary", description = "Read-only summary of a client's active loans.")
@RequiredArgsConstructor
public class ClientLoanSummaryApiResource {

    private static final String RESOURCE_NAME_FOR_PERMISSIONS = "LOAN";

    private final PlatformSecurityContext context;
    private final ClientLoanSummaryReadPlatformService clientLoanSummaryReadPlatformService;
    private final DefaultToApiJsonSerializer<ClientLoanSummaryData> toApiJsonSerializer;

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve a client's active loan summary", operationId = "retrieveClientLoanSummary", description = "Lists the client's active loans with id, product name, principal, outstanding balance, next installment due date and status.\n\nExample Request:\n\nclients/1/loansummary\n\nnextInstallmentDueDate is omitted when the loan has no outstanding installment (null fields are not serialized, as in other Fineract endpoints).")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ClientLoanSummaryApiResourceSwagger.GetClientsClientIdLoanSummaryResponse.class))))
    @ApiResponse(responseCode = "404", description = "Client not found")
    public String retrieveClientLoanSummary(
            @PathParam("clientId") @Parameter(description = "clientId", required = true, example = "1") final Long clientId) {
        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);
        return this.toApiJsonSerializer.serialize(this.clientLoanSummaryReadPlatformService.retrieveActiveLoanSummaries(clientId));
    }
}
