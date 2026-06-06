package com.incode.thirdparty.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiError", description = "Standard Spring error body returned for non-2xx responses")
public record ApiError(
        @Schema(description = "Moment the error was produced (ISO-8601)", example = "2026-06-05T10:30:45.123+00:00")
                String timestamp,
        @Schema(description = "HTTP status code", example = "503") int status,
        @Schema(description = "HTTP status reason phrase", example = "Service Unavailable") String error,
        @Schema(description = "Human-readable explanation of the error", example = "FREE provider unavailable")
                String message,
        @Schema(description = "Request path that produced the error", example = "/free-third-party") String path) {}
