package com.incode.verification.api;

import com.incode.verification.domain.VerificationRecord;
import com.incode.verification.orchestration.VerificationService;
import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/backend-service")
@Tag(name = "Backend service", description = "Main verification API (role VERIFIER or ADMIN)")
@SecurityRequirement(name = "basic")
public class BackendServiceController {

    private static final String VERIFICATION_ID_BAGGAGE = "verificationId";

    private final VerificationService verificationService;
    private final Tracer tracer;

    public BackendServiceController(VerificationService verificationService, Tracer tracer) {
        this.verificationService = verificationService;
        this.tracer = tracer;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VERIFIER', 'ADMIN')")
    @Operation(summary = "Run a verification: FREE first, fall back to PREMIUM, persist the record")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Verification executed and persisted"),
        @ApiResponse(
                responseCode = "400",
                description = "Blank query, missing parameter, or verificationId is not a valid GUID",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiError.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"timestamp\":\"2026-06-05T10:30:45.123Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"query must not be blank\"}"))),
        @ApiResponse(
                responseCode = "401",
                description = "Missing or invalid Basic credentials",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiError.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"timestamp\":\"2026-06-05T10:30:45.123Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Unauthorized\"}"))),
        @ApiResponse(
                responseCode = "403",
                description = "Authenticated user lacks the VERIFIER or ADMIN role",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiError.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"timestamp\":\"2026-06-05T10:30:45.123Z\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Forbidden\"}"))),
        @ApiResponse(
                responseCode = "409",
                description = "A verification already exists for this verificationId",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiError.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"timestamp\":\"2026-06-05T10:30:45.123Z\",\"status\":409,\"error\":\"Conflict\",\"message\":\"Verification already exists: <id>\"}"))),
        @ApiResponse(
                responseCode = "500",
                description = "Unexpected server error",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiError.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"timestamp\":\"2026-06-05T10:30:45.123Z\",\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Internal Server Error\"}")))
    })
    public BackendResponse verify(
            @RequestParam UUID verificationId,
            @RequestParam @NotBlank(message = "query must not be blank") String query) {
        String id = verificationId.toString();
        Span span = tracer.nextSpan().name("backend-service");
        try (Tracer.SpanInScope ignoredScope = tracer.withSpan(span.start());
                BaggageInScope ignoredBaggage = tracer.createBaggageInScope(VERIFICATION_ID_BAGGAGE, id)) {
            VerificationRecord record = verificationService.verify(id, query);
            return BackendResponse.from(record);
        } finally {
            span.end();
        }
    }
}
