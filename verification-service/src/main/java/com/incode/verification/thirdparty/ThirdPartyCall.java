package com.incode.verification.thirdparty;

import com.incode.verification.domain.Source;
import com.incode.verification.metrics.MetricsRecorder;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
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
    private final RetryRegistry retries;

    ThirdPartyCall(MetricsRecorder metrics, CircuitBreakerRegistry circuitBreakers, RetryRegistry retries) {
        this.metrics = metrics;
        this.circuitBreakers = circuitBreakers;
        this.retries = retries;
    }

    ThirdPartyResult execute(
            Source source, String resilienceName, boolean retryOnFailure, Supplier<ThirdPartyResult> call) {
        long start = System.nanoTime();
        String outcome = MetricsRecorder.OUTCOME_SUCCESS;
        try {
            return guarded(resilienceName, retryOnFailure, call).get();
        } catch (CallNotPermittedException circuitOpen) {
            outcome = MetricsRecorder.OUTCOME_UNAVAILABLE;
            log.warn("Circuit open for {} provider, skipping call to {}", source, resilienceName);
            return ThirdPartyResult.unavailable();
        } catch (FeignException exception) {
            if (exception.status() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
                outcome = MetricsRecorder.OUTCOME_UNAVAILABLE;
                log.warn("{} provider unavailable (503) for {}", source, resilienceName);
                return ThirdPartyResult.unavailable();
            }
            log.error("Unexpected error calling {} provider ({})", source, resilienceName, exception);
            throw exception;
        } finally {
            metrics.thirdPartyCall(source, outcome, Duration.ofNanos(System.nanoTime() - start));
        }
    }

    private Supplier<ThirdPartyResult> guarded(String name, boolean retryOnFailure, Supplier<ThirdPartyResult> call) {
        CircuitBreaker breaker = circuitBreakers.circuitBreaker(name);
        Supplier<ThirdPartyResult> attempted =
                retryOnFailure ? Retry.decorateSupplier(retries.retry(name), call) : call;
        return CircuitBreaker.decorateSupplier(breaker, attempted);
    }
}
