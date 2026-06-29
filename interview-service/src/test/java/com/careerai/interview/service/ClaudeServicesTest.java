package com.careerai.interview.service;

import com.careerai.interview.domain.entity.InterviewQuestion;
import com.careerai.interview.domain.entity.InterviewSession;
import com.careerai.interview.domain.enums.InterviewType;
import com.careerai.interview.dto.ai.AnswerEvaluation;
import com.careerai.interview.dto.ai.FeedbackResult;
import com.careerai.interview.dto.ai.QuestionResult;
import com.careerai.interview.exception.AiGenerationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Claude-backed services using a mocked Spring AI {@link ChatClient} chain.
 * Verifies prompt-response parsing, markdown-fence stripping, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClaudeServicesTest {

    @Mock private ChatClient.Builder builder;
    @Mock private ChatClient chatClient;
    @Mock private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock private ChatClient.CallResponseSpec callSpec;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
    }

    private InterviewSession session() {
        return InterviewSession.builder()
                .id(UUID.randomUUID())
                .jobTitle("Backend Engineer")
                .targetCompany("Acme")
                .type(InterviewType.TECHNICAL)
                .totalQuestions(5)
                .build();
    }

    @Test
    void generateQuestion_parsesFencedJson() {
        when(callSpec.content()).thenReturn("""
                ```json
                {"questionText":"Explain ACID.","type":"TECHNICAL","difficulty":"MEDIUM",
                 "skillsTested":["Databases"],"idealAnswer":"Atomicity..."}
                ```
                """);
        var service = new ClaudeQuestionGenerationServiceImpl(builder, objectMapper);

        QuestionResult result = service.generateQuestion(session(), 1, List.of());

        assertThat(result.questionText()).isEqualTo("Explain ACID.");
        assertThat(result.difficulty()).isEqualTo("MEDIUM");
        assertThat(result.skillsTested()).containsExactly("Databases");
    }

    @Test
    void generateQuestion_emptyResponse_throws() {
        when(callSpec.content()).thenReturn("   ");
        var service = new ClaudeQuestionGenerationServiceImpl(builder, objectMapper);

        assertThatThrownBy(() -> service.generateQuestion(session(), 1, List.of()))
                .isInstanceOf(AiGenerationException.class);
    }

    @Test
    void generateHint_returnsContent() {
        when(callSpec.content()).thenReturn("Think about transactions.");
        var service = new ClaudeQuestionGenerationServiceImpl(builder, objectMapper);

        assertThat(service.generateHint("Explain ACID.", "atomicity"))
                .isEqualTo("Think about transactions.");
    }

    @Test
    void evaluateAnswer_parsesJson() {
        when(callSpec.content()).thenReturn(
                "{\"score\":72,\"feedback\":\"Decent\",\"strengths\":\"clarity\",\"improvements\":\"depth\"}");
        var service = new ClaudeFeedbackServiceImpl(builder, objectMapper);

        AnswerEvaluation eval = service.evaluateAnswer("Q", "A", "ideal", "Backend Engineer");

        assertThat(eval.score()).isEqualTo(72);
        assertThat(eval.feedback()).isEqualTo("Decent");
    }

    @Test
    void generateFinalFeedback_parsesJson() {
        when(callSpec.content()).thenReturn("""
                {"technicalScore":80,"behaviouralScore":70,"communicationScore":75,
                 "problemSolvingScore":78,"overallScore":76,"strongAreas":["DSA"],
                 "improvementAreas":["Design"],"detailedFeedback":"Good",
                 "recommendedResources":[{"title":"DDIA","url":"http://x","type":"BOOK"}]}
                """);
        var service = new ClaudeFeedbackServiceImpl(builder, objectMapper);

        InterviewQuestion q = InterviewQuestion.builder()
                .questionNumber(1).difficulty("MEDIUM").answerScore(80).questionText("Q1").build();
        FeedbackResult result = service.generateFinalFeedback(session(), List.of(q));

        assertThat(result.overallScore()).isEqualTo(76);
        assertThat(result.strongAreas()).containsExactly("DSA");
        assertThat(result.recommendedResources()).hasSize(1);
        assertThat(result.recommendedResources().get(0).title()).isEqualTo("DDIA");
    }

    @Test
    void evaluateAnswer_malformedJson_throws() {
        when(callSpec.content()).thenReturn("not json at all");
        var service = new ClaudeFeedbackServiceImpl(builder, objectMapper);

        assertThatThrownBy(() -> service.evaluateAnswer("Q", "A", "ideal", "Backend Engineer"))
                .isInstanceOf(AiGenerationException.class);
    }
}
