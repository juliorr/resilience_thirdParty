package com.incode.thirdparty.api;

import com.incode.thirdparty.config.AppProperties;
import com.incode.thirdparty.data.CompanyDataStore;
import com.incode.thirdparty.data.FailureSimulator;
import com.incode.thirdparty.domain.Company;
import com.incode.thirdparty.domain.Source;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/premium-third-party")
@Tag(
        name = "PREMIUM third party (mock)",
        description = "Open mock provider returning camelCase data; 503 ~10% of calls")
public class PremiumThirdPartyController {

    private final CompanyDataStore dataStore;
    private final FailureSimulator failureSimulator;
    private final double failureProbability;

    public PremiumThirdPartyController(
            CompanyDataStore dataStore, FailureSimulator failureSimulator, AppProperties properties) {
        this.dataStore = dataStore;
        this.failureSimulator = failureSimulator;
        this.failureProbability = properties.failure().premiumProbability();
    }

    @GetMapping
    @Operation(summary = "Search companies whose CIN contains the query (camelCase, ~10% 503)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Matching companies (camelCase; empty list if none)"),
        @ApiResponse(
                responseCode = "503",
                description = "Simulated provider outage (~10% of calls)",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiError.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"timestamp\":\"2026-06-05T10:30:45.123+00:00\",\"status\":503,\"error\":\"Service Unavailable\",\"message\":\"PREMIUM provider unavailable\",\"path\":\"/premium-third-party\"}")))
    })
    public List<PremiumCompany> search(@RequestParam String query) {
        if (failureSimulator.shouldFail(failureProbability)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "PREMIUM provider unavailable");
        }
        return dataStore.search(Source.PREMIUM, query).stream()
                .map(PremiumCompany::from)
                .toList();
    }

    public record PremiumCompany(
            String companyIdentificationNumber,
            String companyName,
            String registrationDate,
            String companyFullAddress,
            boolean isActive) {

        static PremiumCompany from(Company company) {
            return new PremiumCompany(
                    company.cin(), company.name(), company.registrationDate(), company.address(), company.active());
        }
    }
}
