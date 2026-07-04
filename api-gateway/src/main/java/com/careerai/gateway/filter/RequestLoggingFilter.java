package com.careerai.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Access log for every proxied request. Runs last in the pre-phase (order 0); timing is captured
 * before the chain runs and logged from {@code then(...)} once the response completes. Log level
 * scales with latency: INFO under 500ms, WARN under 2s, ERROR at or above 2s.
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    public static final int ORDER = 0;
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final long WARN_THRESHOLD_MS = 500L;
    private static final long ERROR_THRESHOLD_MS = 2_000L;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startNanos = System.nanoTime();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();
        String traceId = exchange.getRequest().getHeaders().getFirst(RequestIdFilter.REQUEST_ID_HEADER);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            String userId = (String) exchange.getAttributes().get(JwtAuthenticationFilter.USER_ID_ATTR);
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            int statusValue = status != null ? status.value() : 0;

            String line = "[TraceId:{}] {} {} userId={} → {} in {}ms";
            Object[] args = {traceId, method, path, userId != null ? userId : "anonymous", statusValue, elapsedMs};

            if (elapsedMs >= ERROR_THRESHOLD_MS) {
                log.error(line, args);
            } else if (elapsedMs >= WARN_THRESHOLD_MS) {
                log.warn(line, args);
            } else {
                log.info(line, args);
            }
        }));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
