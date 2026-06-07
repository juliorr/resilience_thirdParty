package com.incode.verification.api;

import com.incode.verification.domain.VerificationResult;
import com.incode.verification.domain.VerificationResult.Match;
import com.incode.verification.domain.VerificationResult.NoResults;
import com.incode.verification.domain.VerificationResult.ThirdPartiesDown;

public record ResultView(Object result) {

    public static ResultView from(VerificationResult result) {
        return switch (result) {
            case Match match -> new ResultView(MatchView.from(match));
            case NoResults ignored -> new ResultView(new StatusView("NO_RESULTS"));
            case ThirdPartiesDown ignored -> new ResultView(new StatusView("THIRD_PARTIES_DOWN"));
        };
    }

    public record StatusView(String status) {}
}
