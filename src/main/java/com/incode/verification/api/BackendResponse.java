package com.incode.verification.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.incode.verification.domain.VerificationRecord;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BackendResponse(String verificationId, String query, Object result, List<CompanyView> otherResults) {

    public static BackendResponse from(VerificationRecord record) {
        ResultView view = ResultView.from(record.result());
        return new BackendResponse(record.verificationId(), record.queryText(), view.result(), view.otherResults());
    }
}
