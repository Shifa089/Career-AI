package com.careerai.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Minimal OpenAPI metadata for the gateway. The aggregated per-service documentation is surfaced by
 * the {@code springdoc.swagger-ui.urls} dropdown configured in {@code application.yml}; each entry
 * points at that service's own {@code /v3/api-docs}. This bean only labels the gateway's own page.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CareerAI API Gateway")
                        .description("Edge router for the CareerAI platform. Use the definition dropdown "
                                + "to browse each downstream service's API.")
                        .version("v1")
                        .license(new License().name("Proprietary")));
    }
}
