package com.gupta.linkly.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI at /swagger-ui/index.html. The relative server URL keeps "Try it out" working
 * behind Render's HTTPS proxy; the bearer scheme adds an "Authorize" button for JWTs.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Linkly API", version = "1.0",
                description = "Link-in-bio + URL shortener with event-driven click analytics"),
        servers = @Server(url = "/"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
