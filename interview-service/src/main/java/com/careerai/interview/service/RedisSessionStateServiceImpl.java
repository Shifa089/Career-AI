package com.careerai.interview.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed {@link SessionStateService}. Active sessions live under {@code interview:active:{id}}
 * with a 4-hour TTL so abandoned sessions are reclaimed automatically.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedisSessionStateServiceImpl implements SessionStateService {

    static final Duration ACTIVE_TTL = Duration.ofHours(4);

    private final RedisTemplate<String, ActiveSessionState> sessionStateRedisTemplate;

    static String key(UUID sessionId) {
        return "interview:active:" + sessionId;
    }

    @Override
    public void saveActiveSession(UUID sessionId, ActiveSessionState state) {
        sessionStateRedisTemplate.opsForValue().set(key(sessionId), state, ACTIVE_TTL);
    }

    @Override
    public Optional<ActiveSessionState> getActiveSession(UUID sessionId) {
        return Optional.ofNullable(sessionStateRedisTemplate.opsForValue().get(key(sessionId)));
    }

    @Override
    public void appendQuestion(UUID sessionId, int questionNumber, String questionText) {
        ActiveSessionState current = getActiveSession(sessionId).orElse(null);
        if (current == null) {
            log.warn("No active Redis state for session {} when appending question {}", sessionId, questionNumber);
            return;
        }
        List<String> questions = new ArrayList<>(current.previousQuestions());
        questions.add(questionText);
        saveActiveSession(sessionId, new ActiveSessionState(
                current.sessionId(), current.userId(), questionNumber, questions, LocalDateTime.now()));
    }

    @Override
    public void removeActiveSession(UUID sessionId) {
        sessionStateRedisTemplate.delete(key(sessionId));
    }
}
