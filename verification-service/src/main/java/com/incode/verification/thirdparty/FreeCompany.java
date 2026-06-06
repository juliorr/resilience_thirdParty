package com.incode.verification.thirdparty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.incode.verification.domain.Company;

public record FreeCompany(
        String cin,
        String name,
        @JsonProperty("registration_date") String registrationDate,
        String address,
        @JsonProperty("is_active") boolean active) {

    Company toCompany() {
        return new Company(cin, name, registrationDate, address, active);
    }
}
