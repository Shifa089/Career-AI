package com.careerai.interview.service;

import com.careerai.common.exception.ResourceNotFoundException;
import com.careerai.interview.config.KafkaConfig;
import com.careerai.interview.domain.entity.InterviewFeedback;
import com.careerai.interview.domain.entity.InterviewQuestion;
import com.careerai.interview.domain.entity.InterviewSession;
import com.careerai.interview.domain.enums.SessionStatus;
import com.careerai.interview.dto.ai.FeedbackResult;
import com.careerai.interview.dto.request.CreateSessionRequest;
import com.careerai.interview.dto.request.SubmitAnswerRequest;
import com.careerai.interview.dto.response.FeedbackResponse;
import com.careerai.interview.dto.response.InterviewStatsResponse;
import com.careerai.interview.dto.response.QuestionResponse;
import com.careerai.interview.dto.response.SessionResponse;
import com.careerai.interview.event.InterviewCompletedEvent;
import com.careerai.interview.exception.SessionAlreadyActiveException;
import com.careerai.interview.exception.SessionNotFoundException;
import com.careerai.interview.mapper.InterviewMapper;
import com.careerai.interview.repository.InterviewFeedbackRepository;
import com.careerai.interview.repository.InterviewQuestionRepository;
import com.careerai.interview.repository.InterviewSessionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Default {@link InterviewSessionService}. Owns the transactional orchestration of the interview
 * flow; delegates question generation and scoring to Claude services and live progress to Redis.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InterviewSessionServiceImpl implements InterviewSessionService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final QuestionGenerationService questionGenerationService;
    private final FeedbackService feedbackService;
    private final QuestionPrefetchService questionPrefetchService;
    private final AnswerEvaluationService answerEvaluationService;
    private final SessionStateService sessionStateService;
    private final InterviewMapper interviewMapper;
    private final KafkaTemplate<UUID, InterviewCompletedEvent> interviewKafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public SessionResponse createSession(CreateSessionRequest request, UUID userId, String email) {
        InterviewSession session = InterviewSession.builder()
                .userId(userId)
                .userEmail(email)
                .jobTitle(request.jobTitle())
                .jobDescription(request.jobDescription())
                .targetCompany(request.targetCompany())
                .type(request.type())
                .status(SessionStatus.CREATED)
                .totalQuestions(request.totalQuestionsOrDefault())
                .questionsAnswered(0)
                .build();
        session = sessionRepository.save(session);
        log.info("Created interview session {} for user {}", session.getId(), userId);
        return interviewMapper.toSessionResponse(session);
    }

    @Override
    @Transactional
    public QuestionResponse startSession(UUID sessionId, UUID userId) {
        InterviewSession session = ownedSession(sessionId, userId);
        if (session.getStatus() != SessionStatus.CREATED) {
            throw new SessionAlreadyActiveException(
                    "Session %s cannot be started from status %s".formatted(sessionId, session.getStatus()));
        }
        session.setStatus(SessionStatus.ACTIVE);
        session.setStartedAt(LocalDateTime.now());
        sessionRepository.save(session);

        sessionStateService.saveActiveSession(sessionId, new ActiveSessionState(
                sessionId, userId, 0, new ArrayList<>(), LocalDateTime.now()));

        InterviewQuestion question = questionPrefetchService.ensureQuestion(sessionId, 1);
        sessionStateService.saveActiveSession(sessionId, new ActiveSessionState(
                sessionId, userId, 1, new ArrayList<>(List.of(question.getQuestionText())), LocalDateTime.now()));
        return interviewMapper.toQuestionResponse(question);
    }

    @Override
    @Transactional
    public RecordAnswerResult recordAnswer(UUID sessionId, SubmitAnswerRequest request, UUID userId) {
        InterviewSession session = ownedSession(sessionId, userId);
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new SessionAlreadyActiveException(
                    "Session %s is not active (status %s)".formatted(sessionId, session.getStatus()));
        }

        InterviewQuestion question = questionRepository.findById(request.questionId())
                .filter(q -> q.getSession().getId().equals(sessionId))
                .orElseThrow(() -> new ResourceNotFoundException("Interview question", request.questionId()));

        // Persist the raw answer only — no AI call — so scoring/next-question failures downstream can
        // never roll this back and strand the candidate on the same question. Re-answering the same
        // question is idempotent: the counter is not double-incremented and the stale score is cleared
        // so the answer is re-scored.
        boolean alreadyAnswered = question.getUserAnswer() != null;
        question.setUserAnswer(request.answer());
        question.setAnswerScore(null);
        question.setAnswerFeedback(null);
        question.setAnsweredAt(LocalDateTime.now());
        questionRepository.save(question);

        if (!alreadyAnswered) {
            session.setQuestionsAnswered(session.getQuestionsAnswered() + 1);
            sessionRepository.save(session);
        }

        boolean last = question.getQuestionNumber() >= session.getTotalQuestions();
        return new RecordAnswerResult(question.getId(), question.getQuestionNumber(), session.getJobTitle(), last);
    }

    @Override
    @Transactional
    public QuestionResponse provideNextQuestion(UUID sessionId, int nextNumber, UUID userId) {
        ownedSession(sessionId, userId);
        InterviewQuestion question;
        try {
            question = questionPrefetchService.ensureQuestion(sessionId, nextNumber);
        } catch (DataIntegrityViolationException raceLost) {
            // A concurrent prefetch won the create race; use its result.
            question = questionRepository.findBySessionIdAndQuestionNumber(sessionId, nextNumber)
                    .orElseThrow(() -> raceLost);
        }
        sessionStateService.appendQuestion(sessionId, nextNumber, question.getQuestionText());
        return interviewMapper.toQuestionResponse(question);
    }

    @Override
    @Transactional(readOnly = true)
    public String generateHint(UUID sessionId, UUID userId, String partialAnswer) {
        InterviewSession session = ownedSession(sessionId, userId);
        List<InterviewQuestion> questions = questionRepository.findBySessionIdOrderByQuestionNumber(session.getId());
        if (questions.isEmpty()) {
            throw new SessionAlreadyActiveException("Session %s has no active question to hint on".formatted(sessionId));
        }
        InterviewQuestion current = questions.get(questions.size() - 1);
        return questionGenerationService.generateHint(current.getQuestionText(), partialAnswer);
    }

    @Override
    @Transactional
    public FeedbackResponse completeSession(UUID sessionId, UUID userId) {
        InterviewSession session = ownedSession(sessionId, userId);
        if (session.getStatus() == SessionStatus.COMPLETED) {
            return getFeedback(sessionId, userId);
        }
        return finaliseSession(session);
    }

    @Override
    @Transactional
    public FeedbackResponse endSession(UUID sessionId, UUID userId) {
        InterviewSession session = ownedSession(sessionId, userId);
        if (session.getStatus() == SessionStatus.COMPLETED) {
            return getFeedback(sessionId, userId);
        }
        return finaliseSession(session);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SessionResponse> getUserSessions(UUID userId, Pageable pageable) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(interviewMapper::toSessionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SessionResponse getSessionDetails(UUID sessionId, UUID userId) {
        return interviewMapper.toSessionResponse(ownedSession(sessionId, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponse getFeedback(UUID sessionId, UUID userId) {
        ownedSession(sessionId, userId);
        InterviewFeedback feedback = feedbackRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview feedback", sessionId));
        return interviewMapper.toFeedbackResponse(feedback);
    }

    @Override
    @Transactional
    public void abandonSession(UUID sessionId, UUID userId) {
        InterviewSession session = ownedSession(sessionId, userId);
        session.setStatus(SessionStatus.ABANDONED);
        sessionRepository.save(session);
        sessionStateService.removeActiveSession(sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewStatsResponse getUserStats(UUID userId) {
        long total = sessionRepository.countByUserId(userId);
        long completed = sessionRepository.countByUserIdAndStatus(userId, SessionStatus.COMPLETED);
        double completionRate = total == 0 ? 0.0 : (double) completed / total;
        Double avg = sessionRepository.averageOverallScore(userId, SessionStatus.COMPLETED);
        return new InterviewStatsResponse(total, completed, completionRate, avg);
    }

    // ----- helpers -------------------------------------------------------------------------------

    private InterviewSession ownedSession(UUID sessionId, UUID userId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        if (!session.getUserId().equals(userId)) {
            // Don't leak existence to non-owners.
            throw new SessionNotFoundException(sessionId);
        }
        return session;
    }

    /**
     * Finalises a session in a way that can never trap the candidate: outstanding answer scores are
     * backfilled synchronously, the final report is generated with a computed fallback if the AI call
     * fails, and the session is always marked COMPLETED. None of this runs inside the answer-submission
     * transaction, so a failure here leaves the persisted answers intact.
     */
    private FeedbackResponse finaliseSession(InterviewSession session) {
        // Backfill any answered-but-unscored question so the report reflects real per-answer scores,
        // regardless of whether the async scoring finished (or failed).
        List<InterviewQuestion> questions =
                questionRepository.findBySessionIdOrderByQuestionNumber(session.getId());
        for (InterviewQuestion q : questions) {
            if (q.getUserAnswer() != null && q.getAnswerScore() == null) {
                try {
                    answerEvaluationService.evaluateAndStore(q.getId(), session.getJobTitle());
                } catch (Exception e) {
                    log.warn("Backfill scoring failed for question {} (continuing): {}", q.getId(), e.getMessage());
                }
            }
        }
        questions = questionRepository.findBySessionIdOrderByQuestionNumber(session.getId());

        FeedbackResult result = generateFeedbackResilient(session, questions);

        session.setOverallScore(result.overallScore());
        session.setFeedbackSummary(result.detailedFeedback());
        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        sessionRepository.save(session);

        InterviewFeedback feedback = InterviewFeedback.builder()
                .session(session)
                .technicalScore(result.technicalScore())
                .behaviouralScore(result.behaviouralScore())
                .communicationScore(result.communicationScore())
                .problemSolvingScore(result.problemSolvingScore())
                .strongAreas(toJson(result.strongAreas()))
                .improvementAreas(toJson(result.improvementAreas()))
                .detailedFeedback(result.detailedFeedback())
                .recommendedResources(toJsonRaw(result.recommendedResources()))
                .build();
        feedback = feedbackRepository.save(feedback);

        sessionStateService.removeActiveSession(session.getId());
        publishCompleted(session, questions);

        log.info("Completed interview session {} (overallScore={})", session.getId(), result.overallScore());
        return interviewMapper.toFeedbackResponse(feedback);
    }

    /**
     * Publishes the {@code interview.completed} event. A broker outage must never fail session
     * completion (the session + feedback are already persisted), so any Kafka error is logged and
     * swallowed rather than propagated.
     */
    private void publishCompleted(InterviewSession session, List<InterviewQuestion> questions) {
        try {
            Set<String> skills = new LinkedHashSet<>();
            for (InterviewQuestion q : questions) {
                skills.addAll(parseStringList(q.getSkillsTested()));
            }
            InterviewCompletedEvent event = new InterviewCompletedEvent(
                    session.getId(),
                    session.getUserId(),
                    session.getJobTitle(),
                    session.getType(),
                    session.getOverallScore(),
                    session.getTotalQuestions(),
                    new ArrayList<>(skills),
                    session.getCompletedAt());
            interviewKafkaTemplate.send(KafkaConfig.INTERVIEW_COMPLETED_TOPIC, session.getId(), event);
        } catch (Exception e) {
            log.warn("Failed to publish interview.completed event for session {} (session still completed): {}",
                    session.getId(), e.getMessage());
        }
    }

    /**
     * Generates the final report, degrading gracefully. If Claude fails (malformed JSON, timeout,
     * open circuit) we fall back to a report computed from the per-answer scores rather than throwing
     * — a throw here would leave the session un-completed and the candidate stuck on the last question.
     */
    private FeedbackResult generateFeedbackResilient(InterviewSession session, List<InterviewQuestion> questions) {
        try {
            return feedbackService.generateFinalFeedback(session, questions);
        } catch (Exception e) {
            log.error("Final feedback generation failed for session {}; using computed fallback: {}",
                    session.getId(), e.getMessage());
            return fallbackFeedback(questions);
        }
    }

    private FeedbackResult fallbackFeedback(List<InterviewQuestion> questions) {
        int avg = (int) Math.round(questions.stream()
                .map(InterviewQuestion::getAnswerScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0));
        return new FeedbackResult(
                avg, avg, avg, avg, avg,
                List.of("Completed all interview questions"),
                List.of("Detailed AI analysis was unavailable — review your answers and retry for a full report"),
                "Your interview is complete and your answers were scored. The detailed narrative report "
                        + "could not be generated this time, so your overall score reflects the average of your "
                        + "per-answer scores. Retake the interview for a full breakdown.",
                List.of());
    }

    private String toJson(List<String> values) {
        return toJsonRaw(values);
    }

    private String toJsonRaw(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize value to JSON; storing null", e);
            return null;
        }
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(json, STRING_LIST);
            return values != null ? values : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }
}
