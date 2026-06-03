package com.incode.verification.thirdparty;

import com.incode.verification.domain.Source;
import com.incode.verification.metrics.MetricsRecorder;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

final class ThirdPartyCall {

    private ThirdPartyCall() {}

    static ThirdPartyResult measured(MetricsRecorder metrics, Source source, Supplier<ThirdPartyResult> call) {
        long start = System.nanoTime();
        String outcome = MetricsRecorder.OUTCOME_SUCCESS;
        try {
            return call.get();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE)) {
                outcome = MetricsRecorder.OUTCOME_UNAVAILABLE;
                return ThirdPartyResult.unavailable();
            }
            throw exception;
        } finally {
            metrics.thirdPartyCall(source, outcome, Duration.ofNanos(System.nanoTime() - start));
        }
    }
}
