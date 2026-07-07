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

    /**
     * System persona shared by both prompts. Establishes a rigorous, honest examiner so scores
     * actually track answer quality instead of clustering around a bland middle.
     */
    private static final String EVALUATOR_SYSTEM = """
            You are a rigorous, fair, and completely honest senior technical interviewer.
            You grade strictly on the MERIT of the candidate's actual answer — never out of politeness.
            A weak, vague, empty, or wrong answer MUST receive a low score; only genuinely strong,
            correct, specific answers earn high scores. Do not anchor to any default number.
            Use the FULL 0-100 range and make scores discriminating.

            Scoring rubric (apply honestly):
            - 0-15   : no answer, "I don't know", off-topic, or fundamentally incorrect.
            - 16-35  : major misconceptions or mostly missing the point; barely relevant.
            - 36-55  : partially correct but shallow, vague, or missing key required elements.
            - 56-70  : mostly correct and relevant but lacks depth, precision, or concrete examples.
            - 71-85  : solid, correct, well-structured answer covering the key points.
            - 86-100 : excellent — correct, deep, precise, with concrete examples/trade-offs.
            Reward correctness, specificity, structure, and real examples; penalise vagueness,
            filler, hand-waving, and factual errors.
            """;

    private static final String EVALUATE_PROMPT = """
            Evaluate this %s interview answer strictly against the rubric.
            Question: %s
            Key elements a strong answer should contain: %s
            Candidate's answer: %s

            Grade ONLY the candidate's answer above. If it is empty or says nothing of substance,
            the score must be near zero. Respond ONLY in JSON, no prose, no markdown:
            {"score":<0-100 integer>,"feedback":"<2-3 sentence honest assessment>",\
            "strengths":"<what was actually good, or 'none' if nothing>",\
            "improvements":"<specific, actionable gaps>"}
            """;

    private static final String FINAL_PROMPT = """
            Produce an honest overall report for this completed %s interview for the role: %s.
            Per-question difficulty and the score already awarded to each answer: %s

            Base the overall scores on the actual per-answer scores above — do not inflate them.
            If the per-answer scores are low, the overall scores must be correspondingly low.
            Respond ONLY in JSON, no prose, no markdown:
            {"technicalScore":0-100,"behaviouralScore":0-100,\
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
        String content = chatClient.prompt().system(EVALUATOR_SYSTEM).user(prompt).call().content();
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
        String content = chatClient.prompt().system(EVALUATOR_SYSTEM).user(prompt).call().content();
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
