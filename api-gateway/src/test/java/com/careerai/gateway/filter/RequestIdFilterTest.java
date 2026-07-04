package com.careerai.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestIdFilterTest {

    private RequestIdFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RequestIdFilter();
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void generatesRequestId_whenAbsent() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/resumes"));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        String forwarded = captor.getValue().getRequest().getHeaders()
                .getFirst(RequestIdFilter.REQUEST_ID_HEADER);
        assertThat(forwarded).isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst(RequestIdFilter.REQUEST_ID_HEADER))
                .isEqualTo(forwarded);
    }

    @Test
    void preservesRequestId_whenPresent() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/resumes")
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "existing-id"));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertThat(captor.getValue().getRequest().getHeaders()
                .getFirst(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo("existing-id");
        assertThat(exchange.getResponse().getHeaders().getFirst(RequestIdFilter.REQUEST_ID_HEADER))
                .isEqualTo("existing-id");
    }

    @Test
    void orderIsHighestPrecedence() {
        assertThat(filter.getOrder()).isEqualTo(RequestIdFilter.ORDER);
    }
}
