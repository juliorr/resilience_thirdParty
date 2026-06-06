package com.incode.verification.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiError", description = "Standard error body returned for non-2xx responses")
public record ApiError(
        @Schema(description = "Moment the error was produced (ISO-8601)", example = "2026-06-05T10:30:45.123Z")
                String timestamp,
        @Schema(description = "HTTP status code", example = "404") int status,
        @Schema(description = "HTTP status reason phrase", example = "Not Found") String error,
        @Schema(description = "Human-readable explanation of the error", example = "Verification not found: <id>")
                String message) {}
