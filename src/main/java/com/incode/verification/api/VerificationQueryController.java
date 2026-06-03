package com.incode.verification.api;

import com.incode.verification.persistence.VerificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public VerificationDetailResponse byId(@PathVariable String verificationId) {
        String id = RequestValidation.requireUuid(verificationId, "verificationId");
        return repository
                .findById(id)
                .map(VerificationDetailResponse::from)
                .orElseThrow(() -> new VerificationNotFoundException(id));
    }
}
