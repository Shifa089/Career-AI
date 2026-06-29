package com.careerai.interview.service;

import com.careerai.common.exception.ResourceNotFoundException;
import com.careerai.interview.config.KafkaConfig;
import com.careerai.interview.domain.entity.InterviewFeedback;
import com.careerai.interview.domain.entity.InterviewQuestion;
import com.careerai.interview.domain.entity.InterviewSession;
import com.careerai.interview.domain.enums.QuestionType;
import com.careerai.interview.domain.enums.SessionStatus;
import com.careerai.interview.dto.ai.AnswerEvaluation;
import com.careerai.interview.dto.ai.FeedbackResult;
import com.careerai.interview.dto.ai.QuestionResult;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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

        InterviewQuestion question = generateAndSaveQuestion(session, 1, List.of());
        sessionStateService.saveActiveSession(sessionId, new ActiveSessionState(
                sessionId, userId, 1, new ArrayList<>(List.of(question.getQuestionText())), LocalDateTime.now()));
        return interviewMapper.toQuestionResponse(question);
    }

    @Override
    @Transactional
    public SubmitAnswerResult submitAnswer(UUID sessionId, SubmitAnswerRequest request, UUID userId) {
        InterviewSession session = ownedSession(sessionId, userId);
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new SessionAlreadyActiveException(
                    "Session %s is not active (status %s)".formatted(sessionId, session.getStatus()));
        }

        InterviewQuestion question = questionRepository.findById(request.questionId())
                .filter(q -> q.getSession().getId().equals(sessionId))
                .orElseThrow(() -> new ResourceNotFoundException("Interview question", request.questionId()));

        AnswerEvaluation evaluation = feedbackService.evaluateAnswer(
                question.getQuestionText(), request.answer(), question.getIdealAnswer(), session.getJobTitle());

        question.setUserAnswer(request.answer());
        question.setAnswerScore(evaluation.score());
        question.setAnswerFeedback(evaluation.feedback());
        question.setAnsweredAt(LocalDateTime.now());
        questionRepository.save(question);

        session.setQuestionsAnswered(session.getQuestionsAnswered() + 1);

        if (question.getQuestionNumber() < session.getTotalQuestions()) {
            List<String> previous = previousQuestionTexts(sessionId);
            int nextNumber = question.getQuestionNumber() + 1;
            InterviewQuestion next = generateAndSaveQuestion(session, nextNumber, previous);
            sessionRepository.save(session);
            sessionStateService.appendQuestion(sessionId, nextNumber, next.getQuestionText());
            return new SubmitAnswerResult(evaluation.score(), evaluation.feedback(),
                    interviewMapper.toQuestionResponse(next), null, false);
        }

        FeedbackResponse feedback = completeSession(session);
        return new SubmitAnswerResult(evaluation.score(), evaluation.feedback(), null, feedback, true);
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
    public FeedbackResponse endSession(UUID sessionId, UUID userId) {
        InterviewSession session = ownedSession(sessionId, userId);
        if (session.getStatus() == SessionStatus.COMPLETED) {
            return getFeedback(sessionId, userId);
        }
        return completeSession(session);
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

    private InterviewQuestion generateAndSaveQuestion(InterviewSession session, int number, List<String> previous) {
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
    }

    private FeedbackResponse completeSession(InterviewSession session) {
        List<InterviewQuestion> questions =
                questionRepository.findBySessionIdOrderByQuestionNumber(session.getId());
        FeedbackResult result = feedbackService.generateFinalFeedback(session, questions);

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

    private void publishCompleted(InterviewSession session, List<InterviewQuestion> questions) {
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

    private List<String> previousQuestionTexts(UUID sessionId) {
        return questionRepository.findBySessionIdOrderByQuestionNumber(sessionId).stream()
                .map(InterviewQuestion::getQuestionText)
                .toList();
    }
}
