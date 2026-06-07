package com.incode.verification.thirdparty;

import com.incode.verification.domain.Company;
import com.incode.verification.domain.Source;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FreeThirdPartyClient implements ThirdPartyClient {

    private final ThirdPartyApi api;
    private final ThirdPartyCall call;

    public FreeThirdPartyClient(ThirdPartyApi api, ThirdPartyCall call) {
        this.api = api;
        this.call = call;
    }

    @Override
    public Source source() {
        return Source.FREE;
    }

    @Override
    public ThirdPartyResult search(String query) {
        return call.execute(Source.FREE, () -> {
            List<FreeCompany> body = api.searchFree(query);
            List<Company> companies = body == null
                    ? List.of()
                    : body.stream().map(FreeCompany::toCompany).toList();
            return ThirdPartyResult.available(companies);
        });
    }
}
