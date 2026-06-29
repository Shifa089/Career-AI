package com.careerai.jobmatch.service;

import com.careerai.jobmatch.dto.ai.SkillGapResult;
import com.careerai.jobmatch.exception.AiAnalysisException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Claude-backed {@link SkillGapService} using Spring AI's {@link ChatClient}. The model is asked to
 * return strict JSON which is deserialized into a {@link SkillGapResult}. Calls are guarded by
 * Resilience4j retry + circuit breaker (instance {@code anthropic-api}).
 */
@Service
@Slf4j
public class ClaudeSkillGapServiceImpl implements SkillGapService {

    private static final String SKILL_GAP_PROMPT = """
            You are a career advisor. Analyse the skill gap for a candidate applying to %s. \
            Candidate skills: %s. Required skills: %s. \
            Respond ONLY in JSON: {"matchedSkills":[...],"missingSkills":[...],\
            "partialMatches":[{"skill":"...","candidateLevel":"...","requiredLevel":"..."}],\
            "gapScore":0-100,"readinessLevel":"NOT_READY|NEEDS_WORK|ALMOST_READY|READY",\
            "learningPath":[{"skill":"...","priority":"HIGH|MEDIUM|LOW","estimatedWeeks":N,\
            "resources":[{"title":"...","url":"...","type":"COURSE|BOOK|TUTORIAL|PROJECT"}]}],\
            "summary":"..."}
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public ClaudeSkillGapServiceImpl(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    @Retry(name = "anthropic-api")
    @CircuitBreaker(name = "anthropic-api")
    public SkillGapResult analyseSkillGap(List<String> resumeSkills, List<String> requiredSkills, String jobTitle) {
        String prompt = SKILL_GAP_PROMPT.formatted(
                StringUtils.hasText(jobTitle) ? jobTitle : "this role",
                join(resumeSkills),
                join(requiredSkills));

        String content = chatClient.prompt().user(prompt).call().content();
        return parse(content);
    }

    private String join(List<String> skills) {
        return skills == null || skills.isEmpty() ? "none listed" : String.join(", ", skills);
    }

    private SkillGapResult parse(String content) {
        if (!StringUtils.hasText(content)) {
            throw new AiAnalysisException("Claude returned an empty skill-gap response");
        }
        try {
            return objectMapper.readValue(stripFences(content), SkillGapResult.class);
        } catch (Exception e) {
            log.error("Failed to parse skill-gap JSON: {}", content, e);
            throw new AiAnalysisException("Claude returned malformed skill-gap JSON", e);
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
