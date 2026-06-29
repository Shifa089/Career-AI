package com.careerai.interview.service;

import com.careerai.interview.domain.entity.InterviewSession;
import com.careerai.interview.dto.ai.QuestionResult;

import java.util.List;

/**
 * Generates interview questions and hints via Claude.
 */
public interface QuestionGenerationService {

    QuestionResult generateQuestion(InterviewSession session, int questionNumber, List<String> previousQuestions);

    String generateHint(String questionText, String partialAnswer);
}
