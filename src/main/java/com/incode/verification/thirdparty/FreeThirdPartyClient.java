package com.incode.verification.thirdparty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.incode.verification.domain.Company;
import com.incode.verification.domain.Source;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class FreeThirdPartyClient implements ThirdPartyClient {

    private final RestClient restClient;

    public FreeThirdPartyClient(RestClient thirdPartyRestClient) {
        this.restClient = thirdPartyRestClient;
    }

    @Override
    public Source source() {
        return Source.FREE;
    }

    @Override
    public ThirdPartyResult search(String query) {
        try {
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
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE)) {
                return ThirdPartyResult.unavailable();
            }
            throw exception;
        }
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
