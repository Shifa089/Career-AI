package com.careerai.interview.dto.response;

import com.careerai.interview.domain.enums.QuestionType;

import java.util.UUID;

/**
 * A question delivered to the candidate, carrying progress context so the frontend can render a
 * "question {questionNumber} of {totalQuestions}" indicator.
 */
public record QuestionResponse(
        UUID questionId,
        Integer questionNumber,
        String questionText,
        QuestionType type,
        String difficulty,
        Integer totalQuestions
) {
}
