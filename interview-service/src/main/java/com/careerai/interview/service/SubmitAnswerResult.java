package com.careerai.interview.service;

import com.careerai.interview.dto.response.FeedbackResponse;
import com.careerai.interview.dto.response.QuestionResponse;

/**
 * Outcome of submitting an answer: the per-answer evaluation, plus either the next question or
 * (when the session is finished) the final feedback.
 *
 * @param answerScore    the score Claude gave this answer
 * @param answerFeedback Claude's prose feedback on this answer
 * @param nextQuestion   the next question to ask, or {@code null} if the session completed
 * @param finalFeedback  the final session feedback, present only when {@code completed} is true
 * @param completed      whether this answer finished the session
 */
public record SubmitAnswerResult(
        Integer answerScore,
        String answerFeedback,
        QuestionResponse nextQuestion,
        FeedbackResponse finalFeedback,
        boolean completed
) {
}
