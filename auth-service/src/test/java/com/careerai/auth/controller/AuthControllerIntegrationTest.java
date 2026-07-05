package com.careerai.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack auth flow over real Postgres + Redis containers: register, login,
 * refresh, and duplicate-registration handling.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false"
        })
@Testcontainers
class AuthControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("auth_db")
                    .withUsername("careerai")
                    .withPassword("careerai");

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
        // Stable test signing key (>= 256 bits) so JWTs verify across calls.
        registry.add("app.jwt.secret", () -> "test-secret-key-that-is-definitely-long-enough-256-bits!!");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String PASSWORD = "Str0ng!Pass";

    private ResponseEntity<JsonNode> register(String email) {
        String body = """
                {"email":"%s","password":"%s","firstName":"Jane","lastName":"Doe"}
                """.formatted(email, PASSWORD);
        return restTemplate.postForEntity("/api/v1/auth/register", json(body), JsonNode.class);
    }

    @Test
    void register_returns201_withTokens() {
        ResponseEntity<JsonNode> response = register("alice@example.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode data = response.getBody().get("data");
        assertThat(data.get("accessToken").asText()).isNotBlank();
        assertThat(data.get("refreshToken").asText()).isNotBlank();
        assertThat(data.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(data.get("user").get("email").asText()).isEqualTo("alice@example.com");
    }

    @Test
    void login_afterRegister_returnsJwt() {
        register("bob@example.com");

        String body = """
                {"email":"bob@example.com","password":"%s"}
                """.formatted(PASSWORD);
        ResponseEntity<JsonNode> response =
                restTemplate.postForEntity("/api/v1/auth/login", json(body), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("data").get("accessToken").asText()).isNotBlank();
    }

    @Test
    void refresh_afterLogin_returnsNewAccessToken() {
        ResponseEntity<JsonNode> registered = register("carol@example.com");
        String refreshToken = registered.getBody().get("data").get("refreshToken").asText();

        String body = """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);
        ResponseEntity<JsonNode> response =
                restTemplate.postForEntity("/api/v1/auth/refresh", json(body), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String newAccess = response.getBody().get("data").get("accessToken").asText();
        assertThat(newAccess).isNotBlank();
    }

    @Test
    void register_duplicate_returns409() {
        register("dave@example.com");
        ResponseEntity<JsonNode> duplicate = register("dave@example.com");

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody().get("success").asBoolean()).isFalse();
    }

    private static HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
