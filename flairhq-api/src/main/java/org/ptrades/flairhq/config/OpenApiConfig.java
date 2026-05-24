package org.ptrades.flairhq.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "FlairHQ API",
        version = "1.0",
        description = "REST API for FlairHQ — References for r/pokemontrades subreddit. " +
                      "Authentication is via Reddit OAuth2 (/oauth2/authorization/reddit). " +
                      "Once logged in, the session cookie is sent automatically."
    )
)
@SecurityScheme(
    name = "session",
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.COOKIE,
    paramName = "JSESSIONID",
    description = "Session cookie set after Reddit OAuth2 login"
)
public class OpenApiConfig {
}
