package com.careerai.gateway.exception;

import com.careerai.common.dto.ApiResponse;
import com.careerai.common.dto.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * Global error handler for the reactive gateway. A WebFlux {@code @ControllerAdvice} does not catch
 * errors thrown from the routing/filter pipeline, so this is a {@link WebExceptionHandler} ordered
 * ahead of Spring Boot's default (-1) to render every failure as an {@link ApiResponse} JSON body.
 *
 * <p>Maps a downstream {@link ConnectException} to 503 and a {@link TimeoutException} to 504; honours
 * the status of a {@link ResponseStatusException}; everything else is a 500.</p>
 */
@Component
@Order(-2)
public class GatewayExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GatewayExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status;
        String code;
        String message;

        if (ex instanceof ResponseStatusException rse) {
            HttpStatusCode sc = rse.getStatusCode();
            status = HttpStatus.resolve(sc.value()) != null
                    ? HttpStatus.valueOf(sc.value()) : HttpStatus.INTERNAL_SERVER_ERROR;
            code = status.name();
            message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
        } else if (ex instanceof ConnectException || hasCause(ex, ConnectException.class)) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            code = "SERVICE_UNAVAILABLE";
            message = "Service temporarily unavailable";
        } else if (ex instanceof TimeoutException || hasCause(ex, TimeoutException.class)) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            code = "GATEWAY_TIMEOUT";
            message = "Request timed out";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            code = "INTERNAL_ERROR";
            message = "An unexpected error occurred";
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse error = new ErrorResponse(
                status.value(), code, message, exchange.getRequest().getPath().value());
        ApiResponse<Void> body = ApiResponse.error(error);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = ("{\"success\":false,\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private boolean hasCause(Throwable ex, Class<? extends Throwable> type) {
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (type.isInstance(cause)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
