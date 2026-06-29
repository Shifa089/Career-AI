package com.careerai.interview.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Transient interview progress held in Redis while a session is active. Lets the WebSocket flow
 * avoid re-querying the database for question count / prior questions on every turn.
 */
public record ActiveSessionState(
        UUID sessionId,
        UUID userId,
        int currentQuestionNumber,
        List<String> previousQuestions,
        LocalDateTime lastActivity
) {
}
