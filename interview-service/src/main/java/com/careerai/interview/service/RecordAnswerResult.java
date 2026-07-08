package com.careerai.interview.service;

import java.util.UUID;

/**
 * Outcome of persisting a submitted answer, before any AI work happens. The raw answer is already
 * committed at this point; the caller uses these fields to fire background scoring and to decide
 * whether to serve the next question or complete the session.
 *
 * @param questionId     the question that was just answered
 * @param questionNumber its 1-based position in the session
 * @param jobTitle       the session's job title (used to score the answer asynchronously)
 * @param last           whether this was the final question of the session
 */
public record RecordAnswerResult(
        UUID questionId,
        int questionNumber,
        String jobTitle,
        boolean last
) {
}
