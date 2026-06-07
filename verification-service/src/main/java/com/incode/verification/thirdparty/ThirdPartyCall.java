package com.incode.verification.thirdparty;

import com.incode.verification.domain.Source;
import com.incode.verification.metrics.MetricsRecorder;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
class ThirdPartyCall {

    private static final Logger log = LoggerFactory.getLogger(ThirdPartyCall.class);

    private final MetricsRecorder metrics;
    private final CircuitBreakerRegistry circuitBreakers;

    ThirdPartyCall(MetricsRecorder metrics, CircuitBreakerRegistry circuitBreakers) {
        this.metrics = metrics;
        this.circuitBreakers = circuitBreakers;
    }

    ThirdPartyResult execute(Source source, String circuitBreakerName, Supplier<ThirdPartyResult> call) {
        long start = System.nanoTime();
        String outcome = MetricsRecorder.OUTCOME_SUCCESS;
        try {
            CircuitBreaker breaker = circuitBreakers.circuitBreaker(circuitBreakerName);
            return CircuitBreaker.decorateSupplier(breaker, call).get();
        } catch (CallNotPermittedException circuitOpen) {
            outcome = MetricsRecorder.OUTCOME_UNAVAILABLE;
            log.warn("Circuit open for {} provider, skipping call", source);
            return ThirdPartyResult.unavailable();
        } catch (FeignException exception) {
            if (exception.status() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
                outcome = MetricsRecorder.OUTCOME_UNAVAILABLE;
                log.warn("{} provider unavailable (503)", source);
                return ThirdPartyResult.unavailable();
            }
            log.error("Unexpected error calling {} provider", source, exception);
            throw exception;
        } finally {
            metrics.thirdPartyCall(source, outcome, Duration.ofNanos(System.nanoTime() - start));
        }
    }
}
