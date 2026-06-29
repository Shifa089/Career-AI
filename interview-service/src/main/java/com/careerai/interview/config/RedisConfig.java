package com.careerai.interview.config;

import com.careerai.interview.service.ActiveSessionState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis template for the live {@link ActiveSessionState} of in-progress interviews. Keys are
 * strings ({@code interview:active:{id}}); values are JSON serialized with the shared
 * {@link ObjectMapper} (which understands Java time types).
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, ActiveSessionState> sessionStateRedisTemplate(
            RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, ActiveSessionState> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        Jackson2JsonRedisSerializer<ActiveSessionState> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, ActiveSessionState.class);
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
