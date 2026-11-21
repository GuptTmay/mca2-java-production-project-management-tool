package com.taskflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI taskFlowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TaskFlow API")
                        .version("v1")
                        .description(
                                "REST API for TaskFlow, a collaborative project management platform. "
                                        + "Authenticate via POST /api/v1/auth/login or /register, then use the "
                                        + "returned accessToken with the Authorize button below."))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(
                        BEARER_SCHEME_NAME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
