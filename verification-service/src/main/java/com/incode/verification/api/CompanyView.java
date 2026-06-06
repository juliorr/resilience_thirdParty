package com.incode.verification.api;

import com.incode.verification.domain.Company;

public record CompanyView(String cin, String name, String registrationDate, String address, boolean active) {

    public static CompanyView from(Company company) {
        return new CompanyView(
                company.cin(), company.name(), company.registrationDate(), company.address(), company.active());
    }
}
