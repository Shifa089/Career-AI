package com.careerai.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis wiring. Refresh tokens and OTPs are stored as plain JSON/strings, so a
 * {@link StringRedisTemplate} is sufficient; JSON (de)serialization uses the
 * Boot-managed {@link com.fasterxml.jackson.databind.ObjectMapper}.
 */
@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
