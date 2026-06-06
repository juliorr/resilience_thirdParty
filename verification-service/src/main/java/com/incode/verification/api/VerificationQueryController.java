package com.incode.verification.api;

import com.incode.verification.persistence.VerificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/verifications")
@Tag(name = "Verifications", description = "Retrieve persisted verifications (role AUDITOR or ADMIN)")
@SecurityRequirement(name = "basic")
public class VerificationQueryController {

    private final VerificationRepository repository;

    public VerificationQueryController(VerificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{verificationId}")
    @PreAuthorize("hasAnyRole('AUDITOR', 'ADMIN')")
    @Operation(summary = "Fetch a persisted verification by its GUID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Verification found"),
        @ApiResponse(
                responseCode = "400",
                description = "verificationId is not a valid GUID",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiError.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"timestamp\":\"2026-06-05T10:30:45.123Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"verificationId must be a valid GUID\"}"))),
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
                description = "Authenticated user lacks the AUDITOR or ADMIN role",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiError.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"timestamp\":\"2026-06-05T10:30:45.123Z\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Forbidden\"}"))),
        @ApiResponse(
                responseCode = "404",
                description = "No verification exists for the given GUID",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiError.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"timestamp\":\"2026-06-05T10:30:45.123Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Verification not found: <id>\"}"))),
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
    public VerificationDetailResponse byId(@PathVariable UUID verificationId) {
        String id = verificationId.toString();
        return repository
                .findById(id)
                .map(VerificationDetailResponse::from)
                .orElseThrow(() -> new VerificationNotFoundException(id));
    }
}
