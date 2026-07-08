package com.careerai.interview.service;

import com.careerai.interview.domain.entity.InterviewQuestion;
import com.careerai.interview.config.AsyncConfig;
import com.careerai.interview.dto.ai.AnswerEvaluation;
import com.careerai.interview.repository.InterviewQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Scores a submitted answer with Claude on a background thread. Scoring is deliberately kept off the
 * candidate's critical path: the next question is delivered immediately and the score is filled in
 * asynchronously (and backfilled at completion if this ever fails), so the candidate never waits for
 * the previous answer to be analysed.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnswerEvaluationService {

    private final InterviewQuestionRepository questionRepository;
    private final FeedbackService feedbackService;

    @Async(AsyncConfig.INTERVIEW_EXECUTOR)
    @Transactional
    public void evaluateAsync(UUID questionId, String jobTitle) {
        try {
            evaluateAndStore(questionId, jobTitle);
        } catch (Exception e) {
            // Best-effort: a missing score is backfilled synchronously when the session completes.
            log.warn("Async evaluation of question {} failed (will backfill at completion): {}",
                    questionId, e.getMessage());
        }
    }

    /**
     * Evaluates and persists the score/feedback for a single answered question. Skips questions with
     * no answer or that were already scored (idempotent — safe to call from backfill).
     */
    @Transactional
    public void evaluateAndStore(UUID questionId, String jobTitle) {
        InterviewQuestion question = questionRepository.findById(questionId).orElse(null);
        if (question == null || question.getUserAnswer() == null || question.getAnswerScore() != null) {
            return;
        }
        AnswerEvaluation evaluation = feedbackService.evaluateAnswer(
                question.getQuestionText(), question.getUserAnswer(), question.getIdealAnswer(), jobTitle);
        question.setAnswerScore(evaluation.score());
        question.setAnswerFeedback(evaluation.feedback());
        questionRepository.save(question);
    }
}
