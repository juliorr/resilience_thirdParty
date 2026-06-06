package com.incode.verification.thirdparty;

import com.incode.verification.domain.Company;

public record PremiumCompany(
        String companyIdentificationNumber,
        String companyName,
        String registrationDate,
        String companyFullAddress,
        boolean isActive) {

    Company toCompany() {
        return new Company(companyIdentificationNumber, companyName, registrationDate, companyFullAddress, isActive);
    }
}
