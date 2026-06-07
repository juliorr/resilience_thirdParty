package com.incode.verification.thirdparty;

import com.incode.verification.domain.Source;
import com.incode.verification.metrics.MetricsRecorder;
import feign.FeignException;
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

    ThirdPartyCall(MetricsRecorder metrics) {
        this.metrics = metrics;
    }

    ThirdPartyResult execute(Source source, Supplier<ThirdPartyResult> call) {
        long start = System.nanoTime();
        String outcome = MetricsRecorder.OUTCOME_SUCCESS;
        try {
            return call.get();
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
