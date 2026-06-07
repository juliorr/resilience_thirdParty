package com.incode.verification.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.incode.verification.domain.Company;
import com.incode.verification.domain.VerificationResult.Match;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MatchView(
        String cin,
        String name,
        String registrationDate,
        String address,
        boolean active,
        List<CompanyView> otherResults) {

    public static MatchView from(Match match) {
        List<CompanyView> others = match.otherResults().isEmpty()
                ? null
                : match.otherResults().stream().map(CompanyView::from).toList();
        Company company = match.company();
        return new MatchView(
                company.cin(), company.name(), company.registrationDate(), company.address(), company.active(), others);
    }
}
