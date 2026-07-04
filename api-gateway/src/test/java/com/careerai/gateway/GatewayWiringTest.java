package com.careerai.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Boots the full gateway context on a random port and exercises the in-process surface via
 * {@link WebTestClient}: the security chain permits actuator/health, the JWT filter rejects an
 * unauthenticated protected route, and the fallback controller answers. No downstream service or
 * live Redis/Eureka is required — Redis is mocked and Eureka/config are disabled.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "management.health.redis.enabled=false"
})
class GatewayWiringTest {

    @MockBean
    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void actuatorHealth_isPublicAndUp() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void protectedRouteWithoutToken_isUnauthorized() {
        webTestClient.get().uri("/api/v1/resumes")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    void fallbackEndpoint_returnsServiceUnavailable() {
        webTestClient.get().uri("/fallback/auth-service")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.message").value(String::valueOf);
    }
}
