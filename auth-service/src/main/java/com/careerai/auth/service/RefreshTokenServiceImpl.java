package com.careerai.auth.service;

import com.careerai.auth.domain.RefreshToken;
import com.careerai.auth.exception.TokenException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Stores refresh tokens as JSON in Redis under {@code refresh:{token}} with a TTL
 * equal to the remaining lifetime of the token.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void saveRefreshToken(RefreshToken refreshToken) {
        String key = key(refreshToken.getToken());
        Duration ttl = Duration.between(Instant.now(), refreshToken.getExpiresAt());
        if (ttl.isZero() || ttl.isNegative()) {
            log.warn("Refusing to store already-expired refresh token for {}", refreshToken.getEmail());
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(refreshToken);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            throw new TokenException("Failed to serialize refresh token", e);
        }
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        String json = redisTemplate.opsForValue().get(key(token));
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, RefreshToken.class));
        } catch (JsonProcessingException e) {
            throw new TokenException("Failed to deserialize refresh token", e);
        }
    }

    @Override
    public void revokeToken(String token) {
        redisTemplate.delete(key(token));
    }

    @Override
    public boolean isRevoked(String token) {
        return findByToken(token).map(RefreshToken::isRevoked).orElse(true);
    }

    private String key(String token) {
        return KEY_PREFIX + token;
    }
}
