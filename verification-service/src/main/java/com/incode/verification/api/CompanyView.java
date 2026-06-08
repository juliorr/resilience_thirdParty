package com.incode.verification.api;

import com.incode.verification.domain.Company;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CompanyView", description = "Company matching the query")
public record CompanyView(
        @Schema(description = "Company Identification Number", example = "CJQUNXGW") String cin,
        @Schema(description = "Company name", example = "Acme") String name,
        @Schema(description = "Registration date (ISO-8601)", example = "2021-05-01") String registrationDate,
        @Schema(description = "Company address", example = "1 Main St") String address,
        @Schema(description = "Whether the company is active", example = "true") boolean active) {

    public static CompanyView from(Company company) {
        return new CompanyView(
                company.cin(), company.name(), company.registrationDate(), company.address(), company.active());
    }
}
