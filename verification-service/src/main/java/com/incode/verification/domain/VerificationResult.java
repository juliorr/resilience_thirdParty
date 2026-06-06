package com.incode.verification.domain;

import java.util.List;

public sealed interface VerificationResult
        permits VerificationResult.Match, VerificationResult.NoResults, VerificationResult.ThirdPartiesDown {

    record Match(Company company, List<Company> otherResults) implements VerificationResult {}

    record NoResults() implements VerificationResult {}

    record ThirdPartiesDown() implements VerificationResult {}
}
