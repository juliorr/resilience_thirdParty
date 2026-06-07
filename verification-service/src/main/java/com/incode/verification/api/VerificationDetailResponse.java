package com.incode.verification.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.incode.verification.domain.Source;
import com.incode.verification.domain.VerificationRecord;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VerificationDetailResponse(
        String verificationId, String queryText, String timestamp, Source source, Object result) {

    public static VerificationDetailResponse from(VerificationRecord record) {
        ResultView view = ResultView.from(record.result());
        return new VerificationDetailResponse(
                record.verificationId(),
                record.queryText(),
                record.timestamp().toString(),
                record.source(),
                view.result());
    }
}
