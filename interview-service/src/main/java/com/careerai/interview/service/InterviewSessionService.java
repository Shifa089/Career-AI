package com.careerai.interview.service;

import com.careerai.interview.dto.request.CreateSessionRequest;
import com.careerai.interview.dto.request.SubmitAnswerRequest;
import com.careerai.interview.dto.response.FeedbackResponse;
import com.careerai.interview.dto.response.InterviewStatsResponse;
import com.careerai.interview.dto.response.QuestionResponse;
import com.careerai.interview.dto.response.SessionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Orchestrates the interview lifecycle: creation, the Claude-driven question/answer loop,
 * completion + final scoring, and read/stat queries.
 */
public interface InterviewSessionService {

    SessionResponse createSession(CreateSessionRequest request, UUID userId, String email);

    QuestionResponse startSession(UUID sessionId, UUID userId);

    /**
     * Persists the candidate's answer and commits it, without any AI call. Scoring and next-question
     * generation are handled separately so a Claude failure can never roll back the answer.
     */
    RecordAnswerResult recordAnswer(UUID sessionId, SubmitAnswerRequest request, UUID userId);

    /**
     * Returns the next question, using a pre-generated one when available or generating on demand.
     */
    QuestionResponse provideNextQuestion(UUID sessionId, int nextNumber, UUID userId);

    /**
     * Finalises the session: backfills any missing answer scores, builds the final report (with a
     * resilient fallback if the report AI call fails), and marks the session COMPLETED.
     */
    FeedbackResponse completeSession(UUID sessionId, UUID userId);

    String generateHint(UUID sessionId, UUID userId, String partialAnswer);

    FeedbackResponse endSession(UUID sessionId, UUID userId);

    Page<SessionResponse> getUserSessions(UUID userId, Pageable pageable);

    SessionResponse getSessionDetails(UUID sessionId, UUID userId);

    FeedbackResponse getFeedback(UUID sessionId, UUID userId);

    void abandonSession(UUID sessionId, UUID userId);

    InterviewStatsResponse getUserStats(UUID userId);
}
