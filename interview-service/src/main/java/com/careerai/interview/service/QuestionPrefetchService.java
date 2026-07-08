package com.careerai.interview.service;

import com.careerai.common.exception.ResourceNotFoundException;
import com.careerai.interview.config.AsyncConfig;
import com.careerai.interview.domain.entity.InterviewQuestion;
import com.careerai.interview.domain.entity.InterviewSession;
import com.careerai.interview.domain.enums.QuestionType;
import com.careerai.interview.dto.ai.QuestionResult;
import com.careerai.interview.repository.InterviewQuestionRepository;
import com.careerai.interview.repository.InterviewSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Owns generation and persistence of interview questions. Questions depend only on the session and
 * the texts of previously-asked questions (never on the candidate's answers), so the next question
 * can be produced <em>ahead of time</em> — while the candidate is still answering the current one.
 * That pre-generation makes question transitions feel instant instead of blocking on a Claude call.
 *
 * <p>A unique constraint on {@code (session_id, question_number)} makes the concurrent path safe: if
 * the background prefetch and the synchronous fallback both try to create question N, one wins and
 * the other is a no-op.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QuestionPrefetchService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final QuestionGenerationService questionGenerationService;
    private final ObjectMapper objectMapper;

    /**
     * Best-effort background generation of question {@code number}. Any failure (Claude error, open
     * circuit, or losing the create race) is swallowed — the synchronous {@link #ensureQuestion}
     * path will regenerate on demand if the prefetch never landed.
     */
    @Async(AsyncConfig.INTERVIEW_EXECUTOR)
    @Transactional
    public void prefetchAsync(UUID sessionId, int number) {
        try {
            generateIfAbsent(sessionId, number);
            log.debug("Prefetched question #{} for session {}", number, sessionId);
        } catch (DataIntegrityViolationException e) {
            log.debug("Prefetch race for question #{} of session {} — already created", number, sessionId);
        } catch (Exception e) {
            log.warn("Prefetch of question #{} for session {} failed (will generate on demand): {}",
                    number, sessionId, e.getMessage());
        }
    }

    /**
     * Returns question {@code number}, using the pre-generated one if present or generating it
     * synchronously otherwise. Runs in a <em>new</em> transaction so that if it loses the create race
     * with a concurrent prefetch (unique-constraint violation), only this inner transaction is rolled
     * back — the caller's transaction survives and can re-read the winner. See
     * {@code InterviewSessionServiceImpl#provideNextQuestion}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InterviewQuestion ensureQuestion(UUID sessionId, int number) {
        return generateIfAbsent(sessionId, number);
    }

    private InterviewQuestion generateIfAbsent(UUID sessionId, int number) {
        return questionRepository.findBySessionIdAndQuestionNumber(sessionId, number)
                .orElseGet(() -> {
                    InterviewSession session = sessionRepository.findById(sessionId)
                            .orElseThrow(() -> new ResourceNotFoundException("Interview session", sessionId));
                    List<String> previous = questionRepository.findBySessionIdOrderByQuestionNumber(sessionId).stream()
                            .map(InterviewQuestion::getQuestionText)
                            .toList();
                    QuestionResult result = questionGenerationService.generateQuestion(session, number, previous);
                    InterviewQuestion question = InterviewQuestion.builder()
                            .session(session)
                            .questionNumber(number)
                            .questionText(result.questionText())
                            .type(parseType(result.type()))
                            .difficulty(result.difficulty())
                            .idealAnswer(result.idealAnswer())
                            .skillsTested(toJson(result.skillsTested()))
                            .askedAt(LocalDateTime.now())
                            .build();
                    return questionRepository.save(question);
                });
    }

    private QuestionType parseType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return QuestionType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String toJson(List<String> values) {
        if (values == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            log.warn("Failed to serialize skillsTested to JSON; storing null", e);
            return null;
        }
    }
}
