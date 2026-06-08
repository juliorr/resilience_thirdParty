package com.incode.verification.api;

import com.incode.verification.domain.VerificationResult;
import com.incode.verification.domain.VerificationResult.Match;
import com.incode.verification.domain.VerificationResult.NoResults;
import com.incode.verification.domain.VerificationResult.ThirdPartiesDown;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ResultView(Object result, List<CompanyView> otherResults) {

    public static ResultView from(VerificationResult result) {
        return switch (result) {
            case Match match -> new ResultView(CompanyView.from(match.company()), otherResultsOf(match));
            case NoResults ignored -> new ResultView(new StatusView("NO_RESULTS"), null);
            case ThirdPartiesDown ignored -> new ResultView(new StatusView("THIRD_PARTIES_DOWN"), null);
        };
    }

    private static List<CompanyView> otherResultsOf(Match match) {
        return match.otherResults().isEmpty()
                ? null
                : match.otherResults().stream().map(CompanyView::from).toList();
    }

    @Schema(name = "StatusView", description = "Status returned when there is no match")
    public record StatusView(
            @Schema(
                            description = "Reason why there is no match",
                            example = "NO_RESULTS",
                            allowableValues = {"NO_RESULTS", "THIRD_PARTIES_DOWN"})
                    String status) {}
}
