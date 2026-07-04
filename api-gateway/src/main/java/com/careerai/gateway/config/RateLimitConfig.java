package com.careerai.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Wires the rate-limit properties and the atomic sliding-window Lua script used by
 * {@code RateLimitFilter}.
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    /**
     * Atomic sliding-window counter. Evicts entries older than the window, counts what remains, and
     * (only if under the limit) records the current request. Doing this in a single {@code EVAL}
     * removes the check-then-act race and keeps it to one Redis round-trip.
     *
     * <p>KEYS[1] = bucket key. ARGV = [nowMillis, windowMillis, limit, uniqueMember].
     * Returns 1 when the request is allowed, 0 when it must be rejected.</p>
     */
    @Bean
    public RedisScript<Long> rateLimitScript() {
        String lua = """
                local key = KEYS[1]
                local now = tonumber(ARGV[1])
                local window = tonumber(ARGV[2])
                local limit = tonumber(ARGV[3])
                local member = ARGV[4]
                redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
                local count = redis.call('ZCARD', key)
                if count < limit then
                    redis.call('ZADD', key, now, member)
                    redis.call('PEXPIRE', key, window)
                    return 1
                else
                    redis.call('PEXPIRE', key, window)
                    return 0
                end
                """;
        return new DefaultRedisScript<>(lua, Long.class);
    }
}
