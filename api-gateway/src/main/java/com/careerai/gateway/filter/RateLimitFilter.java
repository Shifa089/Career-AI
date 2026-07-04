package com.careerai.gateway.filter;

import com.careerai.gateway.config.RateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

/**
 * Redis-backed sliding-window rate limiter. Runs at order -1, immediately after
 * {@link JwtAuthenticationFilter}, so an authenticated request is keyed by user id (higher limit)
 * and an anonymous one by client IP (lower limit). The window is one minute.
 *
 * <p>The counter is evaluated by an atomic Lua script ({@code rateLimitScript}); if Redis is
 * unavailable the filter <em>fails open</em> — a rate limiter must never take down all traffic.</p>
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    public static final int ORDER = -1;
    private static final long WINDOW_MILLIS = 60_000L;
    private static final long WINDOW_SECONDS = 60L;

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisScript<Long> rateLimitScript;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ReactiveStringRedisTemplate redisTemplate,
                           RedisScript<Long> rateLimitScript,
                           RateLimitProperties properties,
                           ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = rateLimitScript;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userId = (String) exchange.getAttributes().get(JwtAuthenticationFilter.USER_ID_ATTR);

        final String key;
        final int limit;
        if (StringUtils.hasText(userId)) {
            key = "rate:user:" + userId;
            limit = properties.getAuthenticatedRpm();
        } else {
            key = "rate:ip:" + clientIp(exchange);
            limit = properties.getUnauthenticatedRpm();
        }

        long now = System.currentTimeMillis();
        String member = now + "-" + UUID.randomUUID();
        List<String> keys = List.of(key);

        return redisTemplate
                .execute(rateLimitScript, keys,
                        String.valueOf(now), String.valueOf(WINDOW_MILLIS),
                        String.valueOf(limit), member)
                .singleOrEmpty()
                .defaultIfEmpty(1L)
                .flatMap(allowed -> {
                    if (allowed == 0L) {
                        exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(WINDOW_SECONDS));
                        return GatewayResponses.writeError(exchange, HttpStatus.TOO_MANY_REQUESTS,
                                "RATE_LIMIT_EXCEEDED",
                                "Rate limit exceeded. Try again in " + WINDOW_SECONDS + " seconds.",
                                objectMapper);
                    }
                    return chain.filter(exchange);
                })
                // Fail open: never reject traffic because Redis is down.
                .onErrorResume(e -> chain.filter(exchange));
    }

    private String clientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        return remote != null ? remote.getAddress().getHostAddress() : "unknown";
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
