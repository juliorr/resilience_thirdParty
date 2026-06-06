package com.incode.verification.thirdparty;

import com.incode.verification.domain.Company;
import java.util.List;

public record ThirdPartyResult(boolean available, List<Company> companies) {

    public static ThirdPartyResult available(List<Company> companies) {
        return new ThirdPartyResult(true, List.copyOf(companies));
    }

    public static ThirdPartyResult unavailable() {
        return new ThirdPartyResult(false, List.of());
    }

    public boolean hasMatches() {
        return available && !companies.isEmpty();
    }
}
