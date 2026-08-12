package com.example.config;

import com.example.controller.OctoController;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .displayName("Public API")
                .pathsToMatch("/api/**")
                .addOpenApiMethodFilter(method ->
                        !OctoController.class.isAssignableFrom(method.getDeclaringClass()))
                .addOpenApiCustomizer(openApi -> {
                    filterPaths(openApi.getPaths(), path -> path.startsWith("/api/"));
                    ensureComponents(openApi)
                            .addSecuritySchemes(
                                    "BearerAuth",
                                    new SecurityScheme()
                                            .type(SecurityScheme.Type.HTTP)
                                            .scheme("bearer")
                                            .bearerFormat("JWT")
                                            .description("Cognito JWT access token"));
                    openApi.setSecurity(List.of(new SecurityRequirement().addList("BearerAuth")));
                })
                .build();
    }

    @Bean
    public GroupedOpenApi octoApi() {
        return GroupedOpenApi.builder()
                .group("octo-klook")
                .displayName("OCTO / Klook")
                .pathsToMatch("/octo/**")
                .addOpenApiMethodFilter(method ->
                        OctoController.class.isAssignableFrom(method.getDeclaringClass()))
                .addOpenApiCustomizer(openApi -> {
                    filterPaths(openApi.getPaths(), path -> path.startsWith("/octo"));
                    ensureComponents(openApi)
                            .addSecuritySchemes(
                                    "OctoApiKey",
                                    new SecurityScheme()
                                            .type(SecurityScheme.Type.HTTP)
                                            .scheme("bearer")
                                            .bearerFormat("OCTO API Key")
                                            .description(
                                                    "Enter OCTO_API_KEY only. Swagger sends Authorization: Bearer <value>"));
                    openApi.setSecurity(List.of(new SecurityRequirement().addList("OctoApiKey")));
                })
                .build();
    }

    private static Components ensureComponents(io.swagger.v3.oas.models.OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        return openApi.getComponents();
    }

    private static void filterPaths(Paths paths, java.util.function.Predicate<String> keep) {
        if (paths == null) {
            return;
        }
        paths.entrySet().removeIf(entry -> !keep.test(entry.getKey()));
    }
}
