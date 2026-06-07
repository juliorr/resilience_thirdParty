package com.incode.verification.thirdparty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incode.verification.metrics.MetricsRecorder;
import feign.FeignException;
import feign.Request;
import feign.Response;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
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

    private final MetricsRecorder metrics = new MetricsRecorder(new SimpleMeterRegistry());

    private ThirdPartyCall callWith(CircuitBreakerConfig circuitBreaker) {
        return new ThirdPartyCall(metrics, CircuitBreakerRegistry.of(circuitBreaker));
    }

    private static CircuitBreakerConfig neverOpens() {
        return CircuitBreakerConfig.custom().minimumNumberOfCalls(1000).build();
    }

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
        FreeThirdPartyClient client = new FreeThirdPartyClient(api, callWith(neverOpens()));
        when(api.searchFree("q")).thenReturn(List.of(new FreeCompany("A1", "Acme", "2020-01-01", "1 Main St", true)));

        ThirdPartyResult result = client.search("q");

        assertThat(result.available()).isTrue();
        assertThat(result.companies()).extracting(company -> company.cin()).containsExactly("A1");
        verify(api, times(1)).searchFree("q");
    }

    @Test
    void mapsServiceUnavailableToUnavailableWithoutRetrying() {
        FreeThirdPartyClient client = new FreeThirdPartyClient(api, callWith(neverOpens()));
        when(api.searchFree("q")).thenThrow(serviceUnavailable());

        ThirdPartyResult result = client.search("q");

        assertThat(result.available()).isFalse();
        verify(api, times(1)).searchFree("q");
    }

    @Test
    void circuitBreakerShortCircuitsAfterRepeatedFailures() {
        CircuitBreakerConfig opensFast = CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .build();
        PremiumThirdPartyClient client = new PremiumThirdPartyClient(api, callWith(opensFast));
        when(api.searchPremium("q")).thenThrow(serviceUnavailable());

        client.search("q");
        client.search("q");
        ThirdPartyResult whenOpen = client.search("q");

        assertThat(whenOpen.available()).isFalse();
        verify(api, times(2)).searchPremium("q");
    }
}
