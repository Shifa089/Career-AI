package com.careerai.interview.service;

import com.careerai.interview.domain.entity.InterviewSession;
import com.careerai.interview.dto.ai.QuestionResult;
import com.careerai.interview.exception.AiGenerationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Claude-backed {@link QuestionGenerationService} using Spring AI's {@link ChatClient}. The model
 * is asked to return strict JSON which is deserialized into a {@link QuestionResult}. Calls are
 * guarded by Resilience4j retry + circuit breaker.
 */
@Service
@Slf4j
public class ClaudeQuestionGenerationServiceImpl implements QuestionGenerationService {

    private static final String QUESTION_PROMPT = """
            You are a senior interviewer at %s hiring for %s. Generate interview question #%d of %d. \
            Type: %s. Previous questions asked: %s. Vary difficulty progressively. \
            Respond ONLY in JSON: {"questionText":"...","type":"TECHNICAL|BEHAVIOURAL|SITUATIONAL|CODING",\
            "difficulty":"EASY|MEDIUM|HARD","skillsTested":["..."],"idealAnswer":"..."}
            """;

    private static final String HINT_PROMPT = """
            For this interview question: %s. The candidate has answered so far: %s. \
            Give ONE helpful hint without revealing the full answer. Be concise (2-3 sentences).
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public ClaudeQuestionGenerationServiceImpl(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    @Retry(name = "anthropic-api")
    @CircuitBreaker(name = "anthropic-api")
    public QuestionResult generateQuestion(InterviewSession session, int questionNumber, List<String> previousQuestions) {
        String company = StringUtils.hasText(session.getTargetCompany()) ? session.getTargetCompany() : "a top company";
        String prompt = QUESTION_PROMPT.formatted(
                company,
                session.getJobTitle(),
                questionNumber,
                session.getTotalQuestions(),
                session.getType(),
                previousQuestions == null || previousQuestions.isEmpty() ? "none" : String.join("; ", previousQuestions));

        String content = chatClient.prompt().user(prompt).call().content();
        return parse(content);
    }

    @Override
    @Retry(name = "anthropic-api")
    @CircuitBreaker(name = "anthropic-api")
    public String generateHint(String questionText, String partialAnswer) {
        String prompt = HINT_PROMPT.formatted(
                questionText,
                StringUtils.hasText(partialAnswer) ? partialAnswer : "nothing yet");
        return chatClient.prompt().user(prompt).call().content();
    }

    private QuestionResult parse(String content) {
        if (!StringUtils.hasText(content)) {
            throw new AiGenerationException("Claude returned an empty question response");
        }
        try {
            return objectMapper.readValue(stripFences(content), QuestionResult.class);
        } catch (Exception e) {
            log.error("Failed to parse question JSON: {}", content, e);
            throw new AiGenerationException("Claude returned malformed question JSON", e);
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
