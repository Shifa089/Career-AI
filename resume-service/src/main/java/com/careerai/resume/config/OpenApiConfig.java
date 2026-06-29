package com.careerai.resume.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI metadata for the resume-service API.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI resumeServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CareerAI Resume Service API")
                        .description("Resume upload, S3 storage, text extraction and AI-powered analysis")
                        .version("1.0.0")
                        .contact(new Contact().name("CareerAI").email("dev@careerai.com"))
                        .license(new License().name("Apache 2.0")));
    }
}
