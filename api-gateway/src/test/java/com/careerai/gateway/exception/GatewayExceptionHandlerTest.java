package com.careerai.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayExceptionHandlerTest {

    private GatewayExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GatewayExceptionHandler(new ObjectMapper().findAndRegisterModules());
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/resumes"));
    }

    @Test
    void connectException_maps503() {
        MockServerWebExchange exchange = exchange();
        StepVerifier.create(handler.handle(exchange, new ConnectException("refused"))).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void wrappedConnectException_maps503() {
        MockServerWebExchange exchange = exchange();
        RuntimeException wrapper = new RuntimeException(new ConnectException("refused"));
        StepVerifier.create(handler.handle(exchange, wrapper)).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void timeoutException_maps504() {
        MockServerWebExchange exchange = exchange();
        StepVerifier.create(handler.handle(exchange, new TimeoutException("slow"))).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }

    @Test
    void responseStatusException_preservesStatus() {
        MockServerWebExchange exchange = exchange();
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "nope");
        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void genericException_maps500() {
        MockServerWebExchange exchange = exchange();
        StepVerifier.create(handler.handle(exchange, new IllegalStateException("boom"))).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
