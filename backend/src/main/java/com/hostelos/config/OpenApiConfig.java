package com.hostelos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI hostelOsApi() {
        return new OpenAPI()
            .info(new Info().title("HostelOS API").version("1.0")
                .description("Log in at POST /api/auth/login, then click Authorize and paste the token."))
            .components(new Components().addSecuritySchemes("bearer",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
