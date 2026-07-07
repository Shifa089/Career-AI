package com.careerai.jobmatch.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

/**
 * HTTP tuning for the Spring AI clients (Anthropic/Claude chat + OpenAI embeddings).
 *
 * <p>Spring AI builds its clients on the auto-configured {@code RestClient}, which — via Apache
 * HttpClient 5 — advertises {@code Accept-Encoding: gzip} but does <em>not</em> transparently
 * decompress the response in this setup. Jackson then tries to parse the raw gzip bytes and fails
 * with "Unexpected end-of-input". Requesting {@code identity} encoding makes the providers return
 * plain JSON.</p>
 */
@Configuration
public class AiHttpConfig {

    @Bean
    RestClientCustomizer aiRestClientIdentityEncoding() {
        return builder -> builder.defaultHeader(HttpHeaders.ACCEPT_ENCODING, "identity");
    }
}
