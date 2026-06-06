package com.incode.verification.domain;

import java.time.Instant;

public record VerificationRecord(
        String verificationId, String queryText, Instant timestamp, Source source, VerificationResult result) {}
