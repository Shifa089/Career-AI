package com.careerai.jobmatch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link WebClient} used to call the external Adzuna jobs API during ingestion.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient adzunaWebClient(@Value("${adzuna.base-url:https://api.adzuna.com/v1/api}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
