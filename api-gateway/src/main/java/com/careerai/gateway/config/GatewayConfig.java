package com.careerai.gateway.config;

import com.careerai.common.security.JwtUtil;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.time.Duration;

/**
 * Programmatic route table for the gateway. Each downstream service is reached via
 * {@code lb://} (Eureka load-balanced). Every HTTP route is wrapped in a resilience4j circuit
 * breaker (with a {@code forward:/fallback/...} target) and a GET-only retry filter. Discovery
 * auto-routing is disabled in {@code application.yml}, so these explicit routes are the only way in.
 *
 * <p>Global cross-cutting concerns (request id, JWT validation, rate limiting, access logging) are
 * applied by {@code GlobalFilter} beans in the {@code filter} package, not here.</p>
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // Auth service: public auth endpoints + authenticated user endpoints.
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**", "/api/v1/users/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c.setName("auth-cb")
                                        .setFallbackUri("forward:/fallback/auth-service"))
                                .retry(rc -> rc.setRetries(3)
                                        .setMethods(HttpMethod.GET)
                                        .setStatuses(HttpStatus.BAD_GATEWAY,
                                                HttpStatus.SERVICE_UNAVAILABLE,
                                                HttpStatus.GATEWAY_TIMEOUT)
                                        .setBackoff(Duration.ofMillis(50), Duration.ofMillis(500), 2, true)))
                        .uri("lb://auth-service"))

                // Resume service.
                .route("resume-service", r -> r
                        .path("/api/v1/resumes/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c.setName("resume-cb")
                                        .setFallbackUri("forward:/fallback/resume-service"))
                                .retry(rc -> rc.setRetries(3)
                                        .setMethods(HttpMethod.GET)
                                        .setStatuses(HttpStatus.BAD_GATEWAY,
                                                HttpStatus.SERVICE_UNAVAILABLE,
                                                HttpStatus.GATEWAY_TIMEOUT)
                                        .setBackoff(Duration.ofMillis(50), Duration.ofMillis(500), 2, true)))
                        .uri("lb://resume-service"))

                // Interview service (REST).
                .route("interview-service", r -> r
                        .path("/api/v1/interviews/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c.setName("interview-cb")
                                        .setFallbackUri("forward:/fallback/interview-service"))
                                .retry(rc -> rc.setRetries(3)
                                        .setMethods(HttpMethod.GET)
                                        .setStatuses(HttpStatus.BAD_GATEWAY,
                                                HttpStatus.SERVICE_UNAVAILABLE,
                                                HttpStatus.GATEWAY_TIMEOUT)
                                        .setBackoff(Duration.ofMillis(50), Duration.ofMillis(500), 2, true)))
                        .uri("lb://interview-service"))

                // Interview service (WebSocket). No circuit breaker/retry — long-lived upgrade.
                .route("interview-ws", r -> r
                        .path("/ws/**")
                        .uri("lb:ws://interview-service"))

                // Job-match service.
                .route("job-match-service", r -> r
                        .path("/api/v1/job-matches/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c.setName("jobmatch-cb")
                                        .setFallbackUri("forward:/fallback/job-match-service"))
                                .retry(rc -> rc.setRetries(3)
                                        .setMethods(HttpMethod.GET)
                                        .setStatuses(HttpStatus.BAD_GATEWAY,
                                                HttpStatus.SERVICE_UNAVAILABLE,
                                                HttpStatus.GATEWAY_TIMEOUT)
                                        .setBackoff(Duration.ofMillis(50), Duration.ofMillis(500), 2, true)))
                        .uri("lb://job-match-service"))

                .build();
    }

    /**
     * {@link JwtUtil} is a plain helper in common-lib (not a Spring bean), so we expose it here for
     * {@code JwtAuthenticationFilter} to inject. The secret must match the one auth-service signs with.
     */
    @Bean
    public JwtUtil jwtUtil(@org.springframework.beans.factory.annotation.Value("${jwt.secret}") String secret) {
        return new JwtUtil(secret);
    }
}
