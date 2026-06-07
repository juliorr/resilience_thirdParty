package com.incode.verification.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.incode.verification.domain.Company;
import com.incode.verification.domain.VerificationResult.Match;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "MatchView", description = "Company matching the query, along with other possible results")
public record MatchView(
        @Schema(description = "Company Identification Number", example = "CJQUNXGW") String cin,
        @Schema(description = "Company name", example = "Acme") String name,
        @Schema(description = "Registration date (ISO-8601)", example = "2021-05-01") String registrationDate,
        @Schema(description = "Company address", example = "1 Main St") String address,
        @Schema(description = "Whether the company is active", example = "true") boolean active,
        @Schema(description = "Other matches found (absent if none)") List<CompanyView> otherResults) {

    public static MatchView from(Match match) {
        List<CompanyView> others = match.otherResults().isEmpty()
                ? null
                : match.otherResults().stream().map(CompanyView::from).toList();
        Company company = match.company();
        return new MatchView(
                company.cin(), company.name(), company.registrationDate(), company.address(), company.active(), others);
    }
}
