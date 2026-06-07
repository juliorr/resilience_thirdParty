package com.incode.verification.thirdparty;

import com.incode.verification.domain.Company;
import com.incode.verification.domain.Source;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PremiumThirdPartyClient implements ThirdPartyClient {

    private static final String CIRCUIT_BREAKER_NAME = "premium";

    private final ThirdPartyApi api;
    private final ThirdPartyCall call;

    public PremiumThirdPartyClient(ThirdPartyApi api, ThirdPartyCall call) {
        this.api = api;
        this.call = call;
    }

    @Override
    public Source source() {
        return Source.PREMIUM;
    }

    @Override
    public ThirdPartyResult search(String query) {
        return call.execute(Source.PREMIUM, CIRCUIT_BREAKER_NAME, () -> {
            List<PremiumCompany> body = api.searchPremium(query);
            List<Company> companies = body == null
                    ? List.of()
                    : body.stream().map(PremiumCompany::toCompany).toList();
            return ThirdPartyResult.available(companies);
        });
    }
}
