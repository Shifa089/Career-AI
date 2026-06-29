package com.careerai.interview.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Payload submitting a candidate's answer to a specific question.
 */
public record SubmitAnswerRequest(
        @NotNull UUID questionId,
        @NotBlank String answer
) {
}
