package com.careerai.interview.config;

import com.careerai.common.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the shared {@link JwtUtil} used to validate JWTs presented on the WebSocket handshake.
 * The secret must match the one the auth-service signs tokens with.
 */
@Configuration
public class JwtConfig {

    @Bean
    public JwtUtil jwtUtil(@Value("${app.jwt.secret}") String secret) {
        return new JwtUtil(secret);
    }
}
