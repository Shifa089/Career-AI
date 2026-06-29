package com.careerai.interview.service;

import java.util.Optional;
import java.util.UUID;

/**
 * Stores the live state of an in-progress interview session in Redis.
 */
public interface SessionStateService {

    void saveActiveSession(UUID sessionId, ActiveSessionState state);

    Optional<ActiveSessionState> getActiveSession(UUID sessionId);

    void appendQuestion(UUID sessionId, int questionNumber, String questionText);

    void removeActiveSession(UUID sessionId);
}
