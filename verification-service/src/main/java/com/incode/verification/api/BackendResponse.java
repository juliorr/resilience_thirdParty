package com.incode.verification.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.incode.verification.domain.VerificationRecord;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BackendResponse(String verificationId, String query, Object result) {

    public static BackendResponse from(VerificationRecord record) {
        ResultView view = ResultView.from(record.result());
        return new BackendResponse(record.verificationId(), record.queryText(), view.result());
    }
}
