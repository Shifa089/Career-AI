package com.careerai.resume.controller;

import com.careerai.resume.dto.ai.ResumeAnalysisResult;
import com.careerai.resume.service.AiAnalysisService;
import com.careerai.resume.service.FileStorageService;
import com.careerai.resume.service.S3UploadResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Full-stack resume flow over real Postgres, Redis and Kafka containers. S3 and the AI provider
 * are mocked so the test exercises the controller, persistence, security headers and async wiring
 * without external dependencies.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false"
        })
@Testcontainers
class ResumeControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("resume_db")
                    .withUsername("careerai")
                    .withPassword("careerai");

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.ai.openai.api-key", () -> "test-key");
    }

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoBean
    private AiAnalysisService aiAnalysisService;

    @Autowired
    private TestRestTemplate restTemplate;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void upload_returns202_andPersistsResume() {
        stubExternalServices();

        ResponseEntity<JsonNode> upload = uploadResume();

        assertThat(upload.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        JsonNode data = upload.getBody().get("data");
        assertThat(data.get("originalFileName").asText()).isEqualTo("resume.txt");
        assertThat(data.get("userId").asText()).isEqualTo(USER_ID.toString());

        ResponseEntity<JsonNode> list = restTemplate.exchange(
                "/api/v1/resumes", org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(identityHeaders()), JsonNode.class);

        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody().get("data")).hasSize(1);
    }

    @Test
    void upload_withoutIdentityHeaders_returns401() {
        stubExternalServices();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<JsonNode> response =
                restTemplate.postForEntity("/api/v1/resumes", new HttpEntity<>(body, headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void stubExternalServices() {
        when(fileStorageService.uploadFile(any(), any()))
                .thenReturn(new S3UploadResult("resumes/" + USER_ID + "/x.txt", "http://s3/x.txt", "text/plain", 32));
        when(aiAnalysisService.analyseResume(any(), any())).thenReturn(new ResumeAnalysisResult(
                90, "Great fit", 6, "MASTER", List.of("Engineer"), List.of("Java"), List.of("none"),
                List.of("add metrics"), List.of("Spring"), List.of("Go"), List.of()));
    }

    private ResponseEntity<JsonNode> uploadResume() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource());
        body.add("targetRole", "Backend Engineer");

        HttpHeaders headers = identityHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return restTemplate.postForEntity("/api/v1/resumes", new HttpEntity<>(body, headers), JsonNode.class);
    }

    private ByteArrayResource fileResource() {
        return new ByteArrayResource("John Doe — Senior Java Engineer with 6 years of Spring experience.".getBytes()) {
            @Override
            public String getFilename() {
                return "resume.txt";
            }
        };
    }

    private HttpHeaders identityHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", USER_ID.toString());
        headers.add("X-User-Email", "jane@example.com");
        headers.add("X-User-Roles", "ROLE_USER");
        return headers;
    }
}
