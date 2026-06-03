package com.incode.verification.thirdparty;

import com.incode.verification.domain.Company;
import com.incode.verification.domain.Source;
import com.incode.verification.metrics.MetricsRecorder;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PremiumThirdPartyClient implements ThirdPartyClient {

    private final RestClient restClient;
    private final MetricsRecorder metrics;

    public PremiumThirdPartyClient(RestClient thirdPartyRestClient, MetricsRecorder metrics) {
        this.restClient = thirdPartyRestClient;
        this.metrics = metrics;
    }

    @Override
    public Source source() {
        return Source.PREMIUM;
    }

    @Override
    public ThirdPartyResult search(String query) {
        return ThirdPartyCall.measured(metrics, Source.PREMIUM, () -> {
            List<PremiumCompany> body = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/premium-third-party")
                            .queryParam("query", query)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            List<Company> companies = body == null
                    ? List.of()
                    : body.stream().map(PremiumCompany::toCompany).toList();
            return ThirdPartyResult.available(companies);
        });
    }

    private record PremiumCompany(
            String companyIdentificationNumber,
            String companyName,
            String registrationDate,
            String companyFullAddress,
            boolean isActive) {

        Company toCompany() {
            return new Company(
                    companyIdentificationNumber, companyName, registrationDate, companyFullAddress, isActive);
        }
    }
}
