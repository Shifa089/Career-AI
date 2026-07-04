package com.careerai.gateway.filter;

import com.careerai.gateway.config.RateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class RateLimitFilterTest {

    private ReactiveStringRedisTemplate redisTemplate;
    private RedisScript<Long> script;
    private GatewayFilterChain chain;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(ReactiveStringRedisTemplate.class);
        script = mock(RedisScript.class);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        filter = new RateLimitFilter(redisTemplate, script, new RateLimitProperties(),
                new ObjectMapper().findAndRegisterModules());
    }

    private MockServerWebExchange anonymousExchange() {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/resumes")
                        .remoteAddress(new InetSocketAddress("10.0.0.5", 40000)));
    }

    @Test
    void underLimit_passesThrough() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any()))
                .thenReturn(Flux.just(1L));

        StepVerifier.create(filter.filter(anonymousExchange(), chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void overLimit_returns429WithRetryAfter() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any()))
                .thenReturn(Flux.just(0L));

        MockServerWebExchange exchange = anonymousExchange();
        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("60");
        verify(chain, never()).filter(any());
    }

    @Test
    void redisError_failsOpen() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any()))
                .thenReturn(Flux.error(new RuntimeException("redis down")));

        StepVerifier.create(filter.filter(anonymousExchange(), chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void authenticatedUser_keysByUserId() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any()))
                .thenReturn(Flux.just(1L));

        MockServerWebExchange exchange = anonymousExchange();
        exchange.getAttributes().put(JwtAuthenticationFilter.USER_ID_ATTR, "user-123");

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(),
                any(), any(), any(), any());
        assertThat(keysCaptor.getValue().get(0)).isEqualTo("rate:user:user-123");
    }

    @Test
    void anonymousUser_keysByIp() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any()))
                .thenReturn(Flux.just(1L));

        StepVerifier.create(filter.filter(anonymousExchange(), chain)).verifyComplete();

        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(),
                any(), any(), any(), any());
        assertThat(keysCaptor.getValue().get(0)).isEqualTo("rate:ip:10.0.0.5");
    }
}
