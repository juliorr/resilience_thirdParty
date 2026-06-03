package com.incode.verification.api;

import com.incode.verification.domain.VerificationRecord;
import com.incode.verification.orchestration.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    private final VerificationService verificationService;

    public BackendServiceController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VERIFIER', 'ADMIN')")
    @Operation(summary = "Run a verification: FREE first, fall back to PREMIUM, persist the record")
    public BackendResponse verify(@RequestParam String verificationId, @RequestParam String query) {
        String id = RequestValidation.requireUuid(verificationId, "verificationId");
        String text = RequestValidation.requireNonBlank(query, "query");
        VerificationRecord record = verificationService.verify(id, text);
        return BackendResponse.from(record);
    }
}
