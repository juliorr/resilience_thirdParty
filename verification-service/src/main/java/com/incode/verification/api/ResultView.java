package com.incode.verification.api;

import com.incode.verification.domain.VerificationResult;
import com.incode.verification.domain.VerificationResult.Match;
import com.incode.verification.domain.VerificationResult.NoResults;
import com.incode.verification.domain.VerificationResult.ThirdPartiesDown;
import java.util.List;

public record ResultView(Object result, List<CompanyView> otherResults) {

    public static ResultView from(VerificationResult result) {
        return switch (result) {
            case Match match -> new ResultView(CompanyView.from(match.company()), otherResults(match));
            case NoResults ignored -> new ResultView(new StatusView("NO_RESULTS"), null);
            case ThirdPartiesDown ignored -> new ResultView(new StatusView("THIRD_PARTIES_DOWN"), null);
        };
    }

    private static List<CompanyView> otherResults(Match match) {
        if (match.otherResults().isEmpty()) {
            return null;
        }
        return match.otherResults().stream().map(CompanyView::from).toList();
    }

    public record StatusView(String status) {}
}
