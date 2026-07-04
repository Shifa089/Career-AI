package com.careerai.gateway.filter;

import com.careerai.common.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Validates the access token and forwards a trusted identity to downstream services.
 *
 * <p>Runs at order -2 (after {@link RequestIdFilter}, before {@code RateLimitFilter}) so the resolved
 * user id is available for per-user rate limiting. On <em>every</em> request the inbound
 * {@code X-User-*} headers are stripped first — a client must never be able to spoof identity into an
 * internal service. Public paths (auth entry points, actuator, docs) then pass through without a
 * token; all other paths require a valid {@code ACCESS} JWT.</p>
 *
 * <p>The gateway defines the token contract: {@code sub} = email, {@code userId} claim = user UUID
 * (emitted as {@code X-User-Id}), {@code roles} claim = list of role names, {@code type} = ACCESS.</p>
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_EMAIL_HEADER = "X-User-Email";
    public static final String USER_ROLES_HEADER = "X-User-Roles";
    public static final String REQUEST_TIME_HEADER = "X-Request-Time";

    /** Exchange attribute where the resolved user id is stashed for downstream filters. */
    public static final String USER_ID_ATTR = "gateway.userId";

    public static final int ORDER = -2;

    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/validate",
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/fallback/**");

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Always strip any client-supplied identity headers before anything else.
        ServerHttpRequest stripped = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove(USER_ID_HEADER);
                    h.remove(USER_EMAIL_HEADER);
                    h.remove(USER_ROLES_HEADER);
                    h.remove(REQUEST_TIME_HEADER);
                })
                .build();
        ServerWebExchange strippedExchange = exchange.mutate().request(stripped).build();

        String path = strippedExchange.getRequest().getPath().value();
        if (isPublic(path)) {
            return chain.filter(strippedExchange);
        }

        String authHeader = strippedExchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(strippedExchange, "MISSING_TOKEN", "Authorization header missing");
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        final Claims claims;
        try {
            claims = jwtUtil.parseClaims(token);
        } catch (ExpiredJwtException e) {
            return unauthorized(strippedExchange, "TOKEN_EXPIRED", "Invalid or expired token");
        } catch (Exception e) {
            return unauthorized(strippedExchange, "INVALID_TOKEN", "Invalid or expired token");
        }

        if (!ACCESS_TOKEN_TYPE.equals(claims.get("type", String.class))) {
            return unauthorized(strippedExchange, "INVALID_TOKEN", "Invalid or expired token");
        }

        String userId = claims.get("userId", String.class);
        String email = claims.getSubject();
        String roles = joinRoles(claims);

        ServerHttpRequest authenticated = strippedExchange.getRequest().mutate()
                .header(USER_ID_HEADER, userId != null ? userId : "")
                .header(USER_EMAIL_HEADER, email != null ? email : "")
                .header(USER_ROLES_HEADER, roles)
                .header(REQUEST_TIME_HEADER, String.valueOf(Instant.now().toEpochMilli()))
                .build();

        ServerWebExchange authenticatedExchange = strippedExchange.mutate().request(authenticated).build();
        if (userId != null) {
            authenticatedExchange.getAttributes().put(USER_ID_ATTR, userId);
        }
        return chain.filter(authenticatedExchange);
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @SuppressWarnings("unchecked")
    private String joinRoles(Claims claims) {
        Object raw = claims.get("roles");
        if (raw instanceof List<?> list) {
            return String.join(",", ((List<Object>) list).stream().map(String::valueOf).toList());
        }
        return raw != null ? String.valueOf(raw) : "";
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String code, String message) {
        return GatewayResponses.writeError(exchange, HttpStatus.UNAUTHORIZED, code, message, objectMapper);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
