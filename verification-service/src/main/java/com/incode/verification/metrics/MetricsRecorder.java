package com.incode.verification.metrics;

import com.incode.verification.domain.Source;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class MetricsRecorder {

    public static final String OUTCOME_SUCCESS = "success";
    public static final String OUTCOME_UNAVAILABLE = "unavailable";

    private final MeterRegistry registry;

    public MetricsRecorder(MeterRegistry registry) {
        this.registry = registry;
    }

    public void thirdPartyCall(Source source, String outcome, Duration duration) {
        String sourceTag = source.name().toLowerCase(Locale.ROOT);
        registry.counter("thirdparty_calls_total", "source", sourceTag, "outcome", outcome)
                .increment();
        registry.timer("thirdparty_call_duration_seconds", "source", sourceTag).record(duration);
    }

    public void fallback() {
        registry.counter("verification_fallback_total").increment();
    }

    public void result(String type) {
        registry.counter("verification_result_total", "type", type).increment();
    }
}
