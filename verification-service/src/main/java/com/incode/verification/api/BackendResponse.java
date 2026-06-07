package com.incode.verification.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.incode.verification.api.ResultView.StatusView;
import com.incode.verification.domain.VerificationRecord;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BackendResponse(
        @Schema(description = "Identificador de la verificación persistida", example = "a1b2c3d4")
                String verificationId,
        @Schema(description = "Texto de la consulta original", example = "Acme") String query,
        @Schema(
                        description = "Resultado de la verificación. Siempre es un objeto con una de dos formas: "
                                + "MatchView (coincidencia encontrada) o StatusView "
                                + "(NO_RESULTS / THIRD_PARTIES_DOWN).",
                        anyOf = {MatchView.class, StatusView.class})
                Object result) {

    public static BackendResponse from(VerificationRecord record) {
        ResultView view = ResultView.from(record.result());
        return new BackendResponse(record.verificationId(), record.queryText(), view.result());
    }
}
