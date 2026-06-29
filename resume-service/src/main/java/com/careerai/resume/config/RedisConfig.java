package com.careerai.resume.config;

import com.careerai.resume.dto.response.ResumeAnalysisResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Redis template for caching {@link ResumeAnalysisResponse} payloads as JSON. Keys are strings
 * ({@code analysis:{resumeId}}); values are serialized with a Jackson serializer that understands
 * Java time types (configured via the shared {@link ObjectMapper}).
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, ResumeAnalysisResponse> analysisRedisTemplate(
            RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, ResumeAnalysisResponse> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        Jackson2JsonRedisSerializer<ResumeAnalysisResponse> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, ResumeAnalysisResponse.class);
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
