package com.np3.ledgerai.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI ledgerAiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("LedgerAI API")
                        .version("v1")
                        .description("Double-entry ledger, balance projection, reports and AI advisor"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                // Applies the padlock to every endpoint; paste a Keycloak access token in "Authorize".
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    /**
     * springdoc only marks a property as required when a validation annotation says so, so every response field
     * came out optional in the contract (and as {@code id?: string} in the generated TypeScript). Jackson always
     * writes every record component, null included: every property is therefore present. A value that may be null
     * is declared in its type (OpenAPI 3.1: {@code ["string", "null"]}), not by leaving it out of "required".
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Bean
    public OpenApiCustomizer allPropertiesAreRequired() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            for (Schema<?> schema : openApi.getComponents().getSchemas().values()) {
                Map<String, Schema> properties = schema.getProperties();
                if (properties == null) {
                    continue;
                }
                var required = new TreeSet<String>(properties.keySet());
                if (schema.getRequired() != null) {
                    required.addAll(schema.getRequired());
                }
                schema.setRequired(List.copyOf(required));
            }
        };
    }
}
