package com.careerai.gateway.controller;

import com.careerai.common.dto.ApiResponse;
import com.careerai.common.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Circuit-breaker fallback target. When a downstream service's breaker is open (or a call times out),
 * the route forwards here instead of failing, returning a friendly 503 in the standard envelope.
 *
 * <p>The route forwards the <em>original</em> request verbatim, so this handler must accept every HTTP
 * method the routes carry (POST/PUT/PATCH/DELETE, not just GET). Mapping it GET-only made a tripped
 * breaker on e.g. {@code POST /api/v1/auth/register} surface as a misleading
 * "Request method 'POST' is not supported" 405 instead of the intended 503.</p>
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/{serviceName}")
    public Mono<ResponseEntity<ApiResponse<Void>>> fallback(@PathVariable String serviceName) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "SERVICE_UNAVAILABLE",
                serviceName + " is currently unavailable. Please try again.",
                "/fallback/" + serviceName);
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(error)));
    }
}
