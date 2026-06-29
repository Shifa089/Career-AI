package com.careerai.interview.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Deserialization target for Claude's JSON response when generating an interview question.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QuestionResult(
        String questionText,
        String type,
        String difficulty,
        List<String> skillsTested,
        String idealAnswer
) {
}
