package com.careerai.interview.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Deserialization target for Claude's JSON evaluation of a single answer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnswerEvaluation(
        Integer score,
        String feedback,
        String strengths,
        String improvements
) {
}
