package com.incode.verification.thirdparty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.incode.verification.domain.Company;
import com.incode.verification.domain.Source;
import com.incode.verification.metrics.MetricsRecorder;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FreeThirdPartyClient implements ThirdPartyClient {

    private final RestClient restClient;
    private final MetricsRecorder metrics;

    public FreeThirdPartyClient(RestClient thirdPartyRestClient, MetricsRecorder metrics) {
        this.restClient = thirdPartyRestClient;
        this.metrics = metrics;
    }

    @Override
    public Source source() {
        return Source.FREE;
    }

    @Override
    public ThirdPartyResult search(String query) {
        return ThirdPartyCall.measured(metrics, Source.FREE, () -> {
            List<FreeCompany> body = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/free-third-party")
                            .queryParam("query", query)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            List<Company> companies = body == null
                    ? List.of()
                    : body.stream().map(FreeCompany::toCompany).toList();
            return ThirdPartyResult.available(companies);
        });
    }

    private record FreeCompany(
            String cin,
            String name,
            @JsonProperty("registration_date") String registrationDate,
            String address,
            @JsonProperty("is_active") boolean active) {

        Company toCompany() {
            return new Company(cin, name, registrationDate, address, active);
        }
    }
}
