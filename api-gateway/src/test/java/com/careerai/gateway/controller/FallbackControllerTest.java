package com.careerai.gateway.controller;

import com.careerai.common.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();

    @Test
    void returnsServiceUnavailableWithServiceName() {
        StepVerifier.create(controller.fallback("resume-service"))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    ApiResponse<Void> body = response.getBody();
                    assertThat(body).isNotNull();
                    assertThat(body.success()).isFalse();
                    assertThat(body.error()).isNotNull();
                    assertThat(body.error().message()).contains("resume-service");
                    assertThat(body.error().status()).isEqualTo(503);
                })
                .verifyComplete();
    }
}
