package com.careerai.interview.service;

import com.careerai.interview.domain.entity.InterviewQuestion;
import com.careerai.interview.domain.entity.InterviewSession;
import com.careerai.interview.dto.ai.AnswerEvaluation;
import com.careerai.interview.dto.ai.FeedbackResult;

import java.util.List;

/**
 * Evaluates individual answers and generates final interview feedback via Claude.
 */
public interface FeedbackService {

    AnswerEvaluation evaluateAnswer(String questionText, String userAnswer, String idealAnswer, String jobTitle);

    FeedbackResult generateFinalFeedback(InterviewSession session, List<InterviewQuestion> questions);
}
