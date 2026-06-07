package com.incode.verification.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.incode.verification.domain.Company;
import com.incode.verification.domain.VerificationResult.Match;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "MatchView", description = "Empresa que coincide con la consulta, junto a otros posibles resultados")
public record MatchView(
        @Schema(description = "Company Identification Number", example = "CJQUNXGW") String cin,
        @Schema(description = "Nombre de la empresa", example = "Acme") String name,
        @Schema(description = "Fecha de registro (ISO-8601)", example = "2021-05-01") String registrationDate,
        @Schema(description = "Domicilio de la empresa", example = "1 Main St") String address,
        @Schema(description = "Si la empresa está activa", example = "true") boolean active,
        @Schema(description = "Otras coincidencias encontradas (ausente si no hay)") List<CompanyView> otherResults) {

    public static MatchView from(Match match) {
        List<CompanyView> others = match.otherResults().isEmpty()
                ? null
                : match.otherResults().stream().map(CompanyView::from).toList();
        Company company = match.company();
        return new MatchView(
                company.cin(), company.name(), company.registrationDate(), company.address(), company.active(), others);
    }
}
