package com.incode.verification.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.incode.verification.api.ResultView.StatusView;
import com.incode.verification.domain.VerificationRecord;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BackendResponse(
        @Schema(description = "Identifier of the persisted verification", example = "a1b2c3d4") String verificationId,
        @Schema(description = "Text of the original query", example = "Acme") String query,
        @Schema(
                        description = "Verification result. Always an object with one of two shapes: "
                                + "MatchView (match found) or StatusView "
                                + "(NO_RESULTS / THIRD_PARTIES_DOWN).",
                        anyOf = {MatchView.class, StatusView.class})
                Object result) {

    public static BackendResponse from(VerificationRecord record) {
        ResultView view = ResultView.from(record.result());
        return new BackendResponse(record.verificationId(), record.queryText(), view.result());
    }
}
