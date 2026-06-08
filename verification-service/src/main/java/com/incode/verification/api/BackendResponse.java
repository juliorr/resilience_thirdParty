package com.incode.verification.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.incode.verification.api.ResultView.StatusView;
import com.incode.verification.domain.VerificationRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BackendResponse(
        @Schema(description = "Identifier of the persisted verification", example = "a1b2c3d4") String verificationId,
        @Schema(description = "Text of the original query", example = "Acme") String query,
        @Schema(
                        description = "Verification result. Always an object with one of two shapes: "
                                + "CompanyView (match found) or StatusView "
                                + "(NO_RESULTS / THIRD_PARTIES_DOWN).",
                        anyOf = {CompanyView.class, StatusView.class})
                Object result,
        @Schema(description = "Other matches found (absent if none)") List<CompanyView> otherResults) {

    public static BackendResponse from(VerificationRecord record) {
        ResultView view = ResultView.from(record.result());
        return new BackendResponse(record.verificationId(), record.queryText(), view.result(), view.otherResults());
    }
}
