package com.incode.verification.api;

public class VerificationNotFoundException extends RuntimeException {

    public VerificationNotFoundException(String verificationId) {
        super("Verification not found: " + verificationId);
    }
}
