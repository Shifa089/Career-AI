package com.careerai.gateway.filter;

import com.careerai.common.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-that-is-definitely-long-enough-256-bits!!";
    private static final String USER_ID = UUID.randomUUID().toString();
    private static final String EMAIL = "user@careerai.com";

    private JwtUtil jwtUtil;
    private JwtAuthenticationFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET);
        filter = new JwtAuthenticationFilter(jwtUtil, new ObjectMapper().findAndRegisterModules());
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    private String accessToken() {
        return jwtUtil.generateToken(EMAIL,
                Map.of("userId", USER_ID, "roles", List.of("USER", "ADMIN"), "type", "ACCESS"), 60_000L);
    }

    @Test
    void validToken_injectsIdentityHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/resumes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken()));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        HttpHeaders forwarded = captor.getValue().getRequest().getHeaders();
        assertThat(forwarded.getFirst(JwtAuthenticationFilter.USER_ID_HEADER)).isEqualTo(USER_ID);
        assertThat(forwarded.getFirst(JwtAuthenticationFilter.USER_EMAIL_HEADER)).isEqualTo(EMAIL);
        assertThat(forwarded.getFirst(JwtAuthenticationFilter.USER_ROLES_HEADER)).isEqualTo("USER,ADMIN");
        assertThat(forwarded.getFirst(JwtAuthenticationFilter.REQUEST_TIME_HEADER)).isNotBlank();
        assertThat(captor.getValue().getAttributes().get(JwtAuthenticationFilter.USER_ID_ATTR)).isEqualTo(USER_ID);
    }

    @Test
    void spoofedIdentityHeaders_areStripped() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/resumes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
                        .header(JwtAuthenticationFilter.USER_ID_HEADER, "spoofed-admin-id"));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertThat(captor.getValue().getRequest().getHeaders()
                .getFirst(JwtAuthenticationFilter.USER_ID_HEADER)).isEqualTo(USER_ID);
    }

    @Test
    void missingAuthorizationHeader_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/resumes"));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void expiredToken_returns401() {
        String expired = jwtUtil.generateToken(EMAIL,
                Map.of("userId", USER_ID, "roles", List.of("USER"), "type", "ACCESS"), -1_000L);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/resumes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expired));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void refreshTokenUsedAsAccess_returns401() {
        String refresh = jwtUtil.generateToken(EMAIL,
                Map.of("userId", USER_ID, "roles", List.of("USER"), "type", "REFRESH"), 60_000L);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/resumes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + refresh));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void malformedToken_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/resumes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-jwt"));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void publicPath_passesWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/login")
                        .header(JwtAuthenticationFilter.USER_ID_HEADER, "spoofed"));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        // Even on a public path the spoofed identity header must be stripped.
        assertThat(captor.getValue().getRequest().getHeaders()
                .getFirst(JwtAuthenticationFilter.USER_ID_HEADER)).isNull();
    }
}
