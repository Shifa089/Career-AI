package com.careerai.interview.dto.request;

import com.careerai.interview.domain.enums.InterviewType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload to create a new interview session. {@code totalQuestions} defaults to 10 when omitted.
 */
public record CreateSessionRequest(
        @NotBlank String jobTitle,
        String jobDescription,
        String targetCompany,
        @NotNull InterviewType type,
        @Min(5) @Max(20) Integer totalQuestions
) {
    public int totalQuestionsOrDefault() {
        return totalQuestions == null ? 10 : totalQuestions;
    }
}
