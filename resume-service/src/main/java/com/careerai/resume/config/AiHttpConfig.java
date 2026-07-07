package com.careerai.resume.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

/**
 * HTTP tuning for the Spring AI {@code ChatClient} (OpenAI).
 *
 * <p>Spring AI builds its OpenAI client on the auto-configured {@code RestClient}, which — via
 * Apache HttpClient 5 — advertises {@code Accept-Encoding: gzip} but does <em>not</em> transparently
 * decompress the response in this setup. Jackson then tries to parse the raw gzip bytes and fails
 * with "Unexpected end-of-input", so every analysis is marked FAILED. Requesting {@code identity}
 * encoding makes the provider return plain JSON.</p>
 */
@Configuration
public class AiHttpConfig {

    @Bean
    RestClientCustomizer aiRestClientIdentityEncoding() {
        return builder -> builder.defaultHeader(HttpHeaders.ACCEPT_ENCODING, "identity");
    }
}
