package com.careerai.resume.service;

import com.careerai.resume.dto.ai.ResumeAnalysisResult;
import com.careerai.resume.exception.AiAnalysisException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * OpenAI-backed {@link AiAnalysisService} using Spring AI's {@link ChatClient}. The model is asked
 * to return strict JSON (enforced via {@code response-format: json_object}) which is deserialized
 * into a {@link ResumeAnalysisResult}. Calls are guarded by Resilience4j retry + circuit breaker.
 */
@Service
@Slf4j
public class OpenAiAnalysisServiceImpl implements AiAnalysisService {

    private static final String PROMPT_TEMPLATE = """
            You are an expert ATS (Applicant Tracking System) and career coach. Analyse this resume and respond ONLY with valid JSON matching this exact structure:
            {
              "atsScore": <integer 0-100>,
              "summary": "<2-3 sentence professional summary of the candidate>",
              "yearsOfExperience": <integer>,
              "educationLevel": "<HIGH_SCHOOL|BACHELOR|MASTER|PHD|BOOTCAMP|SELF_TAUGHT>",
              "targetRoles": ["<job title 1>", "<job title 2>", "<job title 3>"],
              "strengths": ["<strength 1>", "<strength 2>", "<strength 3>"],
              "weaknesses": ["<weakness 1>", "<weakness 2>"],
              "suggestions": ["<actionable suggestion 1>", "<actionable suggestion 2>", "<actionable suggestion 3>"],
              "keywords": ["<keyword found in resume>"],
              "missingKeywords": ["<important keyword missing for {targetRole}>"],
              "skills": [
                {
                  "skillName": "<skill>",
                  "category": "<TECHNICAL|SOFT|LANGUAGE|TOOL|FRAMEWORK|CERTIFICATION>",
                  "proficiencyLevel": "<BEGINNER|INTERMEDIATE|ADVANCED|EXPERT>",
                  "yearsUsed": <integer or null>,
                  "inferredFromContext": <boolean>
                }
              ]
            }

            Resume text:
            {resumeText}

            Target role (if provided): {targetRole}
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public OpenAiAnalysisServiceImpl(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    @Retry(name = "openai-api")
    @CircuitBreaker(name = "openai-api")
    public ResumeAnalysisResult analyseResume(String resumeText, String targetRole) {
        String role = StringUtils.hasText(targetRole) ? targetRole : "not specified";
        String prompt = PROMPT_TEMPLATE
                .replace("{resumeText}", resumeText == null ? "" : resumeText)
                .replace("{targetRole}", role);

        String content = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return parse(content);
    }

    private ResumeAnalysisResult parse(String content) {
        if (!StringUtils.hasText(content)) {
            throw new AiAnalysisException("AI model returned an empty response");
        }
        try {
            return objectMapper.readValue(stripFences(content), ResumeAnalysisResult.class);
        } catch (Exception e) {
            log.error("Failed to parse AI analysis JSON: {}", content, e);
            throw new AiAnalysisException("AI model returned malformed analysis JSON", e);
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
