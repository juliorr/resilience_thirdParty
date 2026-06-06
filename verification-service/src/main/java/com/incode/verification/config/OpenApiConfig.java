package com.incode.verification.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI verificationOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Verification Service API")
                                .version("1.0.0")
                                .description(
                                        "Backend verification service orchestrating FREE and PREMIUM third-party lookups. "
                                                + "Roles: VERIFIER runs /backend-service, AUDITOR reads /verifications/{id}, ADMIN both."))
                .components(new Components()
                        .addSecuritySchemes(
                                "basic",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")));
    }
}
