package com.eventr.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("EventR API")
                    .version("1.0.0")
                    .description("Complete Event Management Platform API - Comprehensive endpoints for corporate event planning, registration, check-in, analytics, and administration.")
                    .contact(
                        Contact()
                            .name("EventR Team")
                            .email("support@eventr.com")
                            .url("https://github.com/jpwhite3/eventr")
                    )
                    .license(
                        License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT")
                    )
            )
            .addServersItem(
                Server()
                    .url("http://localhost:8080")
                    .description("Development Server")
            )
            .addServersItem(
                Server()
                    .url("https://api.eventr.com")
                    .description("Production Server")
            )
            .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
            .components(
                io.swagger.v3.oas.models.Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT Authentication")
                    )
            )
    }
}