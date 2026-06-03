package com.incode.verification.thirdparty;

import com.incode.verification.domain.Company;
import com.incode.verification.domain.Source;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class PremiumThirdPartyClient implements ThirdPartyClient {

    private final RestClient restClient;

    public PremiumThirdPartyClient(RestClient thirdPartyRestClient) {
        this.restClient = thirdPartyRestClient;
    }

    @Override
    public Source source() {
        return Source.PREMIUM;
    }

    @Override
    public ThirdPartyResult search(String query) {
        try {
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
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE)) {
                return ThirdPartyResult.unavailable();
            }
            throw exception;
        }
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
