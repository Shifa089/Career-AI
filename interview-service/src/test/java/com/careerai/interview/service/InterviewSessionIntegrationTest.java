package com.careerai.interview.service;

import com.careerai.interview.domain.entity.InterviewSession;
import com.careerai.interview.domain.enums.InterviewType;
import com.careerai.interview.domain.enums.SessionStatus;
import com.careerai.interview.dto.ai.AnswerEvaluation;
import com.careerai.interview.dto.ai.FeedbackResult;
import com.careerai.interview.dto.ai.QuestionResult;
import com.careerai.interview.dto.request.CreateSessionRequest;
import com.careerai.interview.dto.request.SubmitAnswerRequest;
import com.careerai.interview.dto.response.QuestionResponse;
import com.careerai.interview.dto.response.SessionResponse;
import com.careerai.interview.repository.InterviewFeedbackRepository;
import com.careerai.interview.repository.InterviewSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Full create → start → answer → complete flow over real Postgres, Redis and Kafka containers.
 * The Claude services are mocked so the test exercises persistence, Redis state, the Kafka producer
 * and the orchestration logic without external AI calls.
 */
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@Testcontainers
class InterviewSessionIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("interview_db")
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
        registry.add("spring.ai.anthropic.api-key", () -> "test-key");
    }

    @MockitoBean
    private QuestionGenerationService questionGenerationService;

    @MockitoBean
    private FeedbackService feedbackService;

    @Autowired
    private InterviewSessionService interviewSessionService;

    @Autowired
    private InterviewSessionRepository sessionRepository;

    @Autowired
    private InterviewFeedbackRepository feedbackRepository;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void fullFlow_createStartAnswerComplete_persistsCompletedSessionAndFeedback() {
        when(questionGenerationService.generateQuestion(any(), anyInt(), anyList()))
                .thenReturn(new QuestionResult("Explain CAP theorem.", "TECHNICAL", "MEDIUM",
                        List.of("Distributed Systems"), "Consistency, Availability, Partition tolerance."));
        when(feedbackService.evaluateAnswer(any(), any(), any(), any()))
                .thenReturn(new AnswerEvaluation(75, "Reasonable answer", "clarity", "depth"));
        when(feedbackService.generateFinalFeedback(any(), anyList()))
                .thenReturn(new FeedbackResult(78, 70, 80, 76, 76,
                        List.of("Fundamentals"), List.of("System design depth"),
                        "Solid foundational performance.", List.of()));

        SessionResponse created = interviewSessionService.createSession(
                new CreateSessionRequest("Backend Engineer", "Distributed systems role", "Acme",
                        InterviewType.TECHNICAL, 2),
                USER_ID, "jane@example.com");
        assertThat(created.status()).isEqualTo(SessionStatus.CREATED);

        QuestionResponse q1 = interviewSessionService.startSession(created.id(), USER_ID);
        assertThat(q1.questionNumber()).isEqualTo(1);

        SubmitAnswerResult first = interviewSessionService.submitAnswer(
                created.id(), new SubmitAnswerRequest(q1.questionId(), "My first answer"), USER_ID);
        assertThat(first.completed()).isFalse();
        assertThat(first.nextQuestion()).isNotNull();

        SubmitAnswerResult second = interviewSessionService.submitAnswer(
                created.id(), new SubmitAnswerRequest(first.nextQuestion().questionId(), "My second answer"),
                USER_ID);
        assertThat(second.completed()).isTrue();
        assertThat(second.finalFeedback()).isNotNull();
        assertThat(second.finalFeedback().overallScore()).isEqualTo(76);

        InterviewSession persisted = sessionRepository.findById(created.id()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(persisted.getQuestionsAnswered()).isEqualTo(2);
        assertThat(persisted.getCompletedAt()).isNotNull();

        assertThat(feedbackRepository.findBySessionId(created.id())).isPresent();

        // read paths
        assertThat(interviewSessionService.getSessionDetails(created.id(), USER_ID).status())
                .isEqualTo(SessionStatus.COMPLETED);
        assertThat(interviewSessionService.getFeedback(created.id(), USER_ID).overallScore()).isEqualTo(76);
        assertThat(interviewSessionService.getUserSessions(USER_ID, Pageable.ofSize(10)).getTotalElements())
                .isGreaterThanOrEqualTo(1);
        assertThat(interviewSessionService.getUserStats(USER_ID).completedSessions()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void hintAndAbandon_areSupported() {
        when(questionGenerationService.generateQuestion(any(), anyInt(), anyList()))
                .thenReturn(new QuestionResult("Describe a conflict.", "BEHAVIOURAL", "EASY",
                        List.of("Communication"), "Use the STAR method."));
        when(questionGenerationService.generateHint(any(), any())).thenReturn("Structure it as STAR.");

        SessionResponse created = interviewSessionService.createSession(
                new CreateSessionRequest("Engineering Manager", null, null, InterviewType.BEHAVIOURAL, 5),
                USER_ID, "jane@example.com");
        interviewSessionService.startSession(created.id(), USER_ID);

        assertThat(interviewSessionService.generateHint(created.id(), USER_ID, "I once..."))
                .isEqualTo("Structure it as STAR.");

        interviewSessionService.abandonSession(created.id(), USER_ID);
        assertThat(sessionRepository.findById(created.id()).orElseThrow().getStatus())
                .isEqualTo(SessionStatus.ABANDONED);
    }
}
