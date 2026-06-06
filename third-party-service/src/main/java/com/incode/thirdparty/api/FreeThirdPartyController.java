package com.incode.thirdparty.api;

import com.fasterxml.jackson.annotation.JsonProperty;
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
@RequestMapping("/free-third-party")
@Tag(name = "FREE third party (mock)", description = "Open mock provider returning snake_case data; 503 ~40% of calls")
public class FreeThirdPartyController {

    private final CompanyDataStore dataStore;
    private final FailureSimulator failureSimulator;
    private final double failureProbability;

    public FreeThirdPartyController(
            CompanyDataStore dataStore, FailureSimulator failureSimulator, AppProperties properties) {
        this.dataStore = dataStore;
        this.failureSimulator = failureSimulator;
        this.failureProbability = properties.failure().freeProbability();
    }

    @GetMapping
    @Operation(summary = "Search companies whose CIN contains the query (snake_case, ~40% 503)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Matching companies (snake_case; empty list if none)"),
        @ApiResponse(
                responseCode = "503",
                description = "Simulated provider outage (~40% of calls)",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiError.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"timestamp\":\"2026-06-05T10:30:45.123+00:00\",\"status\":503,\"error\":\"Service Unavailable\",\"message\":\"FREE provider unavailable\",\"path\":\"/free-third-party\"}")))
    })
    public List<FreeCompany> search(@RequestParam String query) {
        if (failureSimulator.shouldFail(failureProbability)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "FREE provider unavailable");
        }
        return dataStore.search(Source.FREE, query).stream()
                .map(FreeCompany::from)
                .toList();
    }

    public record FreeCompany(
            String cin,
            String name,
            @JsonProperty("registration_date") String registrationDate,
            String address,
            @JsonProperty("is_active") boolean active) {

        static FreeCompany from(Company company) {
            return new FreeCompany(
                    company.cin(), company.name(), company.registrationDate(), company.address(), company.active());
        }
    }
}
