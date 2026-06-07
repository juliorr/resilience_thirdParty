package com.incode.verification.thirdparty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incode.verification.metrics.MetricsRecorder;
import feign.FeignException;
import feign.Request;
import feign.Response;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThirdPartyCallTest {

    @Mock
    private ThirdPartyApi api;

    private final ThirdPartyCall call = new ThirdPartyCall(new MetricsRecorder(new SimpleMeterRegistry()));

    private static FeignException serviceUnavailable() {
        Request request = Request.create(
                Request.HttpMethod.GET, "http://localhost/third-party", Map.of(), Request.Body.empty(), null);
        Response response = Response.builder()
                .status(503)
                .reason("Service Unavailable")
                .request(request)
                .headers(Map.of())
                .build();
        return FeignException.errorStatus("search", response);
    }

    @Test
    void returnsMappedCompaniesOnSuccess() {
        FreeThirdPartyClient client = new FreeThirdPartyClient(api, call);
        when(api.searchFree("q")).thenReturn(List.of(new FreeCompany("A1", "Acme", "2020-01-01", "1 Main St", true)));

        ThirdPartyResult result = client.search("q");

        assertThat(result.available()).isTrue();
        assertThat(result.companies()).extracting(company -> company.cin()).containsExactly("A1");
        verify(api, times(1)).searchFree("q");
    }

    @Test
    void mapsServiceUnavailableToUnavailableWithoutRetrying() {
        FreeThirdPartyClient client = new FreeThirdPartyClient(api, call);
        when(api.searchFree("q")).thenThrow(serviceUnavailable());

        ThirdPartyResult result = client.search("q");

        assertThat(result.available()).isFalse();
        verify(api, times(1)).searchFree("q");
    }

    @Test
    void premiumMapsServiceUnavailableToUnavailable() {
        PremiumThirdPartyClient client = new PremiumThirdPartyClient(api, call);
        when(api.searchPremium("q")).thenThrow(serviceUnavailable());

        ThirdPartyResult result = client.search("q");

        assertThat(result.available()).isFalse();
        verify(api, times(1)).searchPremium("q");
    }
}
