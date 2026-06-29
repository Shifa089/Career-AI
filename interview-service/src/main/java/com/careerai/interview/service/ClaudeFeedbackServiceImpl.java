package com.careerai.interview.service;

import com.careerai.interview.domain.entity.InterviewQuestion;
import com.careerai.interview.domain.entity.InterviewSession;
import com.careerai.interview.dto.ai.AnswerEvaluation;
import com.careerai.interview.dto.ai.FeedbackResult;
import com.careerai.interview.exception.AiGenerationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Claude-backed {@link FeedbackService} using Spring AI's {@link ChatClient}. Per-answer and final
 * feedback are requested as strict JSON and deserialized. Guarded by Resilience4j retry + breaker.
 */
@Service
@Slf4j
public class ClaudeFeedbackServiceImpl implements FeedbackService {

    private static final String EVALUATE_PROMPT = """
            You are evaluating a %s interview answer. Question: %s. Ideal answer elements: %s. \
            Candidate's answer: %s. \
            Respond ONLY in JSON: {"score":0-100,"feedback":"...","strengths":"...","improvements":"..."}
            """;

    private static final String FINAL_PROMPT = """
            Analyse this completed %s interview for %s. Questions and scores: %s. \
            Generate comprehensive feedback in JSON: {"technicalScore":0-100,"behaviouralScore":0-100,\
            "communicationScore":0-100,"problemSolvingScore":0-100,"overallScore":0-100,\
            "strongAreas":["..."],"improvementAreas":["..."],"detailedFeedback":"...",\
            "recommendedResources":[{"title":"...","url":"...","type":"BOOK|COURSE|ARTICLE"}]}
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public ClaudeFeedbackServiceImpl(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    @Retry(name = "anthropic-api")
    @CircuitBreaker(name = "anthropic-api")
    public AnswerEvaluation evaluateAnswer(String questionText, String userAnswer, String idealAnswer, String jobTitle) {
        String prompt = EVALUATE_PROMPT.formatted(
                jobTitle,
                questionText,
                StringUtils.hasText(idealAnswer) ? idealAnswer : "not provided",
                StringUtils.hasText(userAnswer) ? userAnswer : "(no answer)");
        String content = chatClient.prompt().user(prompt).call().content();
        return parse(content, AnswerEvaluation.class, "answer evaluation");
    }

    @Override
    @Retry(name = "anthropic-api")
    @CircuitBreaker(name = "anthropic-api")
    public FeedbackResult generateFinalFeedback(InterviewSession session, List<InterviewQuestion> questions) {
        String questionsJson = questions.stream()
                .map(q -> "Q%d (%s, score %s): %s".formatted(
                        q.getQuestionNumber(),
                        q.getDifficulty(),
                        q.getAnswerScore(),
                        q.getQuestionText()))
                .collect(Collectors.joining(" | "));
        String prompt = FINAL_PROMPT.formatted(session.getType(), session.getJobTitle(), questionsJson);
        String content = chatClient.prompt().user(prompt).call().content();
        return parse(content, FeedbackResult.class, "final feedback");
    }

    private <T> T parse(String content, Class<T> type, String label) {
        if (!StringUtils.hasText(content)) {
            throw new AiGenerationException("Claude returned an empty " + label + " response");
        }
        try {
            return objectMapper.readValue(stripFences(content), type);
        } catch (Exception e) {
            log.error("Failed to parse {} JSON: {}", label, content, e);
            throw new AiGenerationException("Claude returned malformed " + label + " JSON", e);
        }
    }

    /** Defensive cleanup in case the model wraps JSON in a markdown code fence. */
    private String stripFences(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstBrace = trimmed.indexOf('{');
            int lastBrace = trimmed.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                return trimmed.substring(firstBrace, lastBrace + 1);
            }
        }
        return trimmed;
    }
}
