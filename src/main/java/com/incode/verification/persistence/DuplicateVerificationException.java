package com.incode.verification.persistence;

public class DuplicateVerificationException extends RuntimeException {

    public DuplicateVerificationException(String verificationId) {
        super("Verification already exists: " + verificationId);
    }
}
