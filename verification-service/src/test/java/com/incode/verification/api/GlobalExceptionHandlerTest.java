package com.incode.verification.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsUnexpectedExceptionToInternalServerErrorWithoutLeakingDetails() {
        RuntimeException cause = new IllegalStateException("sensitive internal detail");

        ResponseEntity<Map<String, Object>> response = handler.handleUnexpected(cause);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(body.get("error")).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        assertThat(body.get("message")).isEqualTo("Internal server error");
        assertThat(body.toString()).doesNotContain("sensitive internal detail");
    }
}
