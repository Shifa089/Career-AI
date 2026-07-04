package com.careerai.jobmatch;

import com.careerai.jobmatch.event.ResumeAnalysedEvent;
import com.careerai.jobmatch.repository.JobMatchRepository;
import com.careerai.jobmatch.repository.ResumeEmbeddingRepository;
import com.careerai.jobmatch.service.EmbeddingService;
import com.careerai.jobmatch.service.SkillGapService;
import com.careerai.jobmatch.dto.ai.SkillGapResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Full-stack job-match flow over real Postgres (pgvector), Redis and Kafka containers. The embedding
 * model is replaced by a deterministic bag-of-words stub (no OpenAI key needed) and the Claude
 * skill-gap call is mocked, so the test exercises the controller, security headers, the pgvector
 * native similarity query, Redis caching and the Kafka consumer end to end.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.ai.openai.api-key=test-key",
                "spring.ai.anthropic.api-key=test-key"
        })
@Testcontainers
class JobMatchIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("jobmatch_db")
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
    }

    @TestConfiguration
    static class StubAiConfig {
        /** Deterministic bag-of-words embedding so semantically overlapping text scores higher. */
        @Bean
        @Primary
        EmbeddingModel stubEmbeddingModel() {
            return new EmbeddingModel() {
                @Override
                public float[] embed(String text) {
                    float[] v = new float[1536];
                    if (text == null) {
                        return v;
                    }
                    for (String token : text.toLowerCase().split("[^a-z0-9]+")) {
                        if (!token.isBlank()) {
                            v[Math.floorMod(token.hashCode(), 1536)] += 1f;
                        }
                    }
                    return v;
                }

                @Override
                public float[] embed(Document document) {
                    return embed(document.getText());
                }

                @Override
                public EmbeddingResponse call(EmbeddingRequest request) {
                    List<Embedding> embeddings = new ArrayList<>();
                    List<String> instructions = request.getInstructions();
                    for (int i = 0; i < instructions.size(); i++) {
                        embeddings.add(new Embedding(embed(instructions.get(i)), i));
                    }
                    return new EmbeddingResponse(embeddings);
                }
            };
        }
    }

    @MockitoBean
    private SkillGapService skillGapService;

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private EmbeddingService embeddingService;
    @Autowired private ResumeEmbeddingRepository resumeEmbeddingRepository;
    @Autowired private JobMatchRepository jobMatchRepository;
    @Autowired private KafkaTemplate<Object, Object> kafkaTemplate;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void findMatches_returnsPgVectorOrderedResults() {
        UUID resumeId = UUID.randomUUID();
        embeddingService.generateAndSaveResumeEmbedding(resumeId, USER_ID,
                "Senior Backend Engineer Java Spring Boot PostgreSQL Kafka microservices",
                List.of("Java", "Spring Boot", "PostgreSQL", "Kafka"));

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/job-matches/find", HttpMethod.POST,
                new HttpEntity<>("{\"resumeId\":\"" + resumeId + "\",\"limit\":5}", jsonHeaders(identityHeaders())),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().get("data");
        assertThat(data).isNotEmpty();
        // Highest-scoring match first; a backend/Java listing should outrank unrelated ones.
        assertThat(data.get(0).get("similarityScore").asDouble()).isGreaterThan(0.0);
        assertThat(data.get(0).get("matchPercentage").asInt()).isBetween(0, 100);
    }

    @Test
    void findMatches_withoutIdentityHeaders_returns401() {
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/job-matches/find", HttpMethod.POST,
                new HttpEntity<>("{\"resumeId\":\"" + UUID.randomUUID() + "\"}", jsonHeaders(new HttpHeaders())),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void resumeAnalysedEvent_isConsumed_andEmbeddingPersisted() {
        UUID resumeId = UUID.randomUUID();
        ResumeAnalysedEvent event = new ResumeAnalysedEvent(resumeId, USER_ID, "jane@example.com",
                List.of("Python", "Django", "PostgreSQL"), List.of("Backend Engineer"), 88, LocalDateTime.now());

        kafkaTemplate.send("resume.analysed", resumeId, event);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(resumeEmbeddingRepository.findByResumeId(resumeId)).isPresent());
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(jobMatchRepository.findByUserId(USER_ID,
                        org.springframework.data.domain.PageRequest.of(0, 50)).getTotalElements()).isPositive());
    }

    @Test
    void skillGap_endpoint_returnsAnalysis() {
        UUID resumeId = UUID.randomUUID();
        embeddingService.generateAndSaveResumeEmbedding(resumeId, USER_ID,
                "Frontend Engineer React TypeScript", List.of("React", "TypeScript"));
        when(skillGapService.analyseSkillGap(any(), any(), any())).thenReturn(
                new SkillGapResult(List.of("React"), List.of("Next.js"), List.of(), 40, "ALMOST_READY",
                        List.of(), "Almost there"));

        List<JsonNode> matches = findMatches(resumeId);
        assertThat(matches).isNotEmpty();
        String matchId = matches.get(0).get("matchId").asText();

        ResponseEntity<JsonNode> gap = restTemplate.exchange(
                "/api/v1/job-matches/" + matchId + "/skill-gap", HttpMethod.GET,
                new HttpEntity<>(identityHeaders()), JsonNode.class);

        assertThat(gap.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(gap.getBody().get("data").get("readinessLevel").asText()).isEqualTo("ALMOST_READY");
    }

    private List<JsonNode> findMatches(UUID resumeId) {
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/job-matches/find", HttpMethod.POST,
                new HttpEntity<>("{\"resumeId\":\"" + resumeId + "\",\"limit\":5}", jsonHeaders(identityHeaders())),
                JsonNode.class);
        List<JsonNode> list = new ArrayList<>();
        response.getBody().get("data").forEach(list::add);
        return list;
    }

    private HttpHeaders identityHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", USER_ID.toString());
        headers.add("X-User-Email", "jane@example.com");
        headers.add("X-User-Roles", "ROLE_USER");
        return headers;
    }

    private HttpHeaders jsonHeaders(HttpHeaders base) {
        base.add("Content-Type", "application/json");
        return base;
    }
}
