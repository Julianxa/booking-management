package com.example.config;


import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomizer;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/**")
                .addOpenApiCustomizer(securityOpenApiCustomizer())
                .build();
    }

    @Bean
    public OpenApiCustomizer securityOpenApiCustomizer() {
        return openApi -> openApi
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("BearerAuth"))
                .getComponents()
                .addSecuritySchemes("BearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization"));
    }
}
