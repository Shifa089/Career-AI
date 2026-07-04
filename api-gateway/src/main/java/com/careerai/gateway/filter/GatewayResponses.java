package com.careerai.gateway.filter;

import com.careerai.common.dto.ApiResponse;
import com.careerai.common.dto.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Shared helper for short-circuiting a request with a JSON {@link ApiResponse} error body from a
 * reactive filter. Status and content-type are set before the body is written (headers commit on the
 * first flush), and the returned {@code Mono<Void>} completes the exchange — callers must return it
 * and must not also invoke the filter chain.
 */
final class GatewayResponses {

    private GatewayResponses() {
    }

    static Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String code,
                                 String message, ObjectMapper mapper) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse error = new ErrorResponse(
                status.value(), code, message, exchange.getRequest().getPath().value());
        ApiResponse<Void> body = ApiResponse.error(error);

        byte[] bytes;
        try {
            bytes = mapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = ("{\"success\":false,\"message\":\"" + message + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
