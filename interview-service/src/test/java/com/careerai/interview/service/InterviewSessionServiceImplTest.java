package com.careerai.interview.service;

import com.careerai.interview.config.KafkaConfig;
import com.careerai.interview.domain.entity.InterviewFeedback;
import com.careerai.interview.domain.entity.InterviewQuestion;
import com.careerai.interview.domain.entity.InterviewSession;
import com.careerai.interview.domain.enums.InterviewType;
import com.careerai.interview.domain.enums.QuestionType;
import com.careerai.interview.domain.enums.SessionStatus;
import com.careerai.interview.dto.ai.FeedbackResult;
import com.careerai.interview.dto.request.CreateSessionRequest;
import com.careerai.interview.dto.request.SubmitAnswerRequest;
import com.careerai.interview.dto.response.FeedbackResponse;
import com.careerai.interview.dto.response.QuestionResponse;
import com.careerai.interview.dto.response.SessionResponse;
import com.careerai.interview.event.InterviewCompletedEvent;
import com.careerai.interview.mapper.InterviewMapper;
import com.careerai.interview.repository.InterviewFeedbackRepository;
import com.careerai.interview.repository.InterviewQuestionRepository;
import com.careerai.interview.repository.InterviewSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InterviewSessionServiceImplTest {

    @Mock private InterviewSessionRepository sessionRepository;
    @Mock private InterviewQuestionRepository questionRepository;
    @Mock private InterviewFeedbackRepository feedbackRepository;
    @Mock private QuestionGenerationService questionGenerationService;
    @Mock private FeedbackService feedbackService;
    @Mock private QuestionPrefetchService questionPrefetchService;
    @Mock private AnswerEvaluationService answerEvaluationService;
    @Mock private SessionStateService sessionStateService;
    @Mock private InterviewMapper interviewMapper;
    @Mock private KafkaTemplate<UUID, InterviewCompletedEvent> interviewKafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private InterviewSessionServiceImpl service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "jane@example.com";

    @BeforeEach
    void setUp() {
        service = new InterviewSessionServiceImpl(
                sessionRepository, questionRepository, feedbackRepository,
                questionGenerationService, feedbackService, questionPrefetchService,
                answerEvaluationService, sessionStateService,
                interviewMapper, interviewKafkaTemplate, objectMapper);
    }

    @Test
    void createSession_savesEntityAndReturnsResponse() {
        CreateSessionRequest request = new CreateSessionRequest(
                "Backend Engineer", "Build APIs", "Acme", InterviewType.TECHNICAL, null);
        when(sessionRepository.save(any(InterviewSession.class))).thenAnswer(inv -> {
            InterviewSession s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        SessionResponse mapped = sampleSessionResponse();
        when(interviewMapper.toSessionResponse(any(InterviewSession.class))).thenReturn(mapped);

        SessionResponse response = service.createSession(request, USER_ID, EMAIL);

        assertThat(response).isSameAs(mapped);
        ArgumentCaptor<InterviewSession> captor = ArgumentCaptor.forClass(InterviewSession.class);
        verify(sessionRepository).save(captor.capture());
        InterviewSession saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(SessionStatus.CREATED);
        assertThat(saved.getTotalQuestions()).isEqualTo(10); // default applied
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void recordAnswer_notLastQuestion_persistsAnswerWithoutAiCall() {
        InterviewSession session = activeSession(10);
        InterviewQuestion question = answeredQuestionStub(session, 1);

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(questionRepository.save(any(InterviewQuestion.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordAnswerResult result = service.recordAnswer(
                session.getId(), new SubmitAnswerRequest(question.getId(), "my answer"), USER_ID);

        assertThat(result.last()).isFalse();
        assertThat(result.questionNumber()).isEqualTo(1);
        assertThat(result.jobTitle()).isEqualTo("Backend Engineer");
        assertThat(question.getUserAnswer()).isEqualTo("my answer");
        assertThat(session.getQuestionsAnswered()).isEqualTo(1);
        // No blocking AI call happens while recording the answer.
        verify(feedbackService, never()).evaluateAnswer(any(), any(), any(), any());
        verify(feedbackService, never()).generateFinalFeedback(any(), anyList());
    }

    @Test
    void recordAnswer_lastQuestion_flagsLast() {
        InterviewSession session = activeSession(1);
        InterviewQuestion question = answeredQuestionStub(session, 1);

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(questionRepository.save(any(InterviewQuestion.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordAnswerResult result = service.recordAnswer(
                session.getId(), new SubmitAnswerRequest(question.getId(), "final answer"), USER_ID);

        assertThat(result.last()).isTrue();
        assertThat(session.getQuestionsAnswered()).isEqualTo(1);
    }

    @Test
    void provideNextQuestion_returnsPrefetchedQuestion() {
        InterviewSession session = activeSession(10);
        InterviewQuestion next = answeredQuestionStub(session, 2);

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(questionPrefetchService.ensureQuestion(session.getId(), 2)).thenReturn(next);
        when(interviewMapper.toQuestionResponse(next))
                .thenReturn(new QuestionResponse(next.getId(), 2, "Next?", QuestionType.TECHNICAL, "MEDIUM", 10));

        QuestionResponse response = service.provideNextQuestion(session.getId(), 2, USER_ID);

        assertThat(response.questionNumber()).isEqualTo(2);
        verify(sessionStateService).appendQuestion(eq(session.getId()), eq(2), any());
    }

    @Test
    void completeSession_generatesReportAndPublishesEvent() {
        InterviewSession session = activeSession(1);
        InterviewQuestion question = answeredQuestionStub(session, 1);
        question.setUserAnswer("final answer");
        question.setAnswerScore(90); // already scored → no backfill

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(questionRepository.findBySessionIdOrderByQuestionNumber(session.getId()))
                .thenReturn(List.of(question));
        when(feedbackService.generateFinalFeedback(eq(session), anyList()))
                .thenReturn(new FeedbackResult(85, 80, 88, 82, 84,
                        List.of("Problem solving"), List.of("System design"),
                        "Strong overall.", List.of()));
        when(feedbackRepository.save(any(InterviewFeedback.class))).thenAnswer(inv -> {
            InterviewFeedback f = inv.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });
        FeedbackResponse mappedFeedback = sampleFeedbackResponse(session.getId());
        when(interviewMapper.toFeedbackResponse(any(InterviewFeedback.class))).thenReturn(mappedFeedback);

        FeedbackResponse result = service.completeSession(session.getId(), USER_ID);

        assertThat(result).isSameAs(mappedFeedback);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(session.getOverallScore()).isEqualTo(84);
        assertThat(session.getCompletedAt()).isNotNull();
        verify(feedbackRepository).save(any(InterviewFeedback.class));
        verify(sessionStateService).removeActiveSession(session.getId());
        verify(interviewKafkaTemplate).send(eq(KafkaConfig.INTERVIEW_COMPLETED_TOPIC),
                eq(session.getId()), any(InterviewCompletedEvent.class));
    }

    @Test
    void completeSession_whenFinalFeedbackFails_usesFallbackAndStillCompletes() {
        InterviewSession session = activeSession(1);
        InterviewQuestion question = answeredQuestionStub(session, 1);
        question.setUserAnswer("final answer");
        question.setAnswerScore(70); // already scored → fallback averages this

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(questionRepository.findBySessionIdOrderByQuestionNumber(session.getId()))
                .thenReturn(List.of(question));
        when(feedbackService.generateFinalFeedback(any(), anyList()))
                .thenThrow(new RuntimeException("AI down"));
        when(feedbackRepository.save(any(InterviewFeedback.class))).thenAnswer(inv -> {
            InterviewFeedback f = inv.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });
        FeedbackResponse mappedFeedback = sampleFeedbackResponse(session.getId());
        when(interviewMapper.toFeedbackResponse(any(InterviewFeedback.class))).thenReturn(mappedFeedback);

        FeedbackResponse result = service.completeSession(session.getId(), USER_ID);

        // The session still completes with a computed fallback report — never traps the candidate.
        assertThat(result).isSameAs(mappedFeedback);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(session.getOverallScore()).isEqualTo(70); // average of per-answer scores
        verify(feedbackRepository).save(any(InterviewFeedback.class));
    }

    // ----- helpers -------------------------------------------------------------------------------

    private InterviewSession activeSession(int totalQuestions) {
        return InterviewSession.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .userEmail(EMAIL)
                .jobTitle("Backend Engineer")
                .type(InterviewType.TECHNICAL)
                .status(SessionStatus.ACTIVE)
                .totalQuestions(totalQuestions)
                .questionsAnswered(0)
                .build();
    }

    private InterviewQuestion answeredQuestionStub(InterviewSession session, int number) {
        return InterviewQuestion.builder()
                .id(UUID.randomUUID())
                .session(session)
                .questionNumber(number)
                .questionText("Explain dependency injection.")
                .type(QuestionType.TECHNICAL)
                .difficulty("MEDIUM")
                .idealAnswer("IoC container manages dependencies.")
                .skillsTested("[\"Spring\"]")
                .build();
    }

    private SessionResponse sampleSessionResponse() {
        return new SessionResponse(UUID.randomUUID(), "Backend Engineer", "Acme",
                InterviewType.TECHNICAL, SessionStatus.CREATED, 10, 0, null, null, null, null);
    }

    private FeedbackResponse sampleFeedbackResponse(UUID sessionId) {
        return new FeedbackResponse(UUID.randomUUID(), sessionId, 84, 85, 80, 88, 82,
                List.of("Problem solving"), List.of("System design"), "Strong overall.", List.of(), null);
    }
}
