package com.incode.verification.domain;

public record Company(String cin, String name, String registrationDate, String address, boolean active) {}
