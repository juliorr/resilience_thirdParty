package com.incode.verification.thirdparty;

import feign.RequestInterceptor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ThirdPartyTracingConfig {

    private static final String TRACEPARENT_HEADER = "traceparent";

    @Bean
    RequestInterceptor traceContextPropagationInterceptor(Tracer tracer) {
        return template -> {
            Span span = tracer.currentSpan();
            if (span != null) {
                TraceContext context = span.context();
                template.header(TRACEPARENT_HEADER, "00-" + context.traceId() + "-" + context.spanId() + "-01");
            }
        };
    }
}
