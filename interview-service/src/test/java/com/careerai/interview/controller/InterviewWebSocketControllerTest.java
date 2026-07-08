package com.careerai.interview.controller;

import com.careerai.interview.dto.response.FeedbackResponse;
import com.careerai.interview.dto.response.QuestionResponse;
import com.careerai.interview.dto.websocket.WsIncomingMessage;
import com.careerai.interview.dto.websocket.WsOutgoingMessage;
import com.careerai.interview.security.StompPrincipal;
import com.careerai.interview.service.AnswerEvaluationService;
import com.careerai.interview.service.InterviewSessionService;
import com.careerai.interview.service.QuestionPrefetchService;
import com.careerai.interview.service.RecordAnswerResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class InterviewWebSocketControllerTest {

    @Mock private InterviewSessionService interviewSessionService;
    @Mock private AnswerEvaluationService answerEvaluationService;
    @Mock private QuestionPrefetchService questionPrefetchService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private InterviewWebSocketController controller;

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private final StompPrincipal principal = new StompPrincipal(USER_ID.toString());

    @Test
    void start_sendsQuestionAndPrefetchesNext() {
        QuestionResponse q = new QuestionResponse(UUID.randomUUID(), 1, "Q?", null, "EASY", 5);
        when(interviewSessionService.startSession(SESSION_ID, USER_ID)).thenReturn(q);

        controller.start(SESSION_ID, principal);

        WsOutgoingMessage sent = captureSent();
        assertThat(sent.type()).isEqualTo(WsOutgoingMessage.Type.QUESTION);
        assertThat(sent.payload()).isSameAs(q);
        verify(questionPrefetchService).prefetchAsync(SESSION_ID, 2);
    }

    @Test
    void answer_notCompleted_scoresAsyncAndSendsNextQuestion() {
        UUID questionId = UUID.randomUUID();
        QuestionResponse next = new QuestionResponse(UUID.randomUUID(), 2, "Next?", null, "MEDIUM", 5);
        when(interviewSessionService.recordAnswer(eq(SESSION_ID), any(), eq(USER_ID)))
                .thenReturn(new RecordAnswerResult(questionId, 1, "Engineer", false));
        when(interviewSessionService.provideNextQuestion(SESSION_ID, 2, USER_ID)).thenReturn(next);

        WsIncomingMessage msg = new WsIncomingMessage(
                WsIncomingMessage.Type.SUBMIT_ANSWER, "my answer", Map.of("questionId", questionId.toString()));
        controller.answer(SESSION_ID, msg, principal);

        // The answer is scored off the critical path...
        verify(answerEvaluationService).evaluateAsync(questionId, "Engineer");
        // ...and exactly one QUESTION frame is streamed back (no mid-interview FEEDBACK frame).
        WsOutgoingMessage sent = captureSent();
        assertThat(sent.type()).isEqualTo(WsOutgoingMessage.Type.QUESTION);
        assertThat(sent.payload()).isSameAs(next);
        verify(questionPrefetchService).prefetchAsync(SESSION_ID, 3);
        verify(interviewSessionService, never()).completeSession(any(), any());
    }

    @Test
    void answer_lastQuestion_sendsSessionComplete() {
        UUID questionId = UUID.randomUUID();
        FeedbackResponse feedback = new FeedbackResponse(UUID.randomUUID(), SESSION_ID, 80,
                null, null, null, null, null, null, "done", null, null);
        when(interviewSessionService.recordAnswer(eq(SESSION_ID), any(), eq(USER_ID)))
                .thenReturn(new RecordAnswerResult(questionId, 5, "Engineer", true));
        when(interviewSessionService.completeSession(SESSION_ID, USER_ID)).thenReturn(feedback);

        WsIncomingMessage msg = new WsIncomingMessage(
                WsIncomingMessage.Type.SUBMIT_ANSWER, "answer", Map.of("questionId", questionId.toString()));
        controller.answer(SESSION_ID, msg, principal);

        verify(answerEvaluationService).evaluateAsync(questionId, "Engineer");
        WsOutgoingMessage sent = captureSent();
        assertThat(sent.type()).isEqualTo(WsOutgoingMessage.Type.SESSION_COMPLETE);
        assertThat(sent.payload()).isSameAs(feedback);
        verify(interviewSessionService, never()).provideNextQuestion(any(), anyInt(), any());
    }

    @Test
    void end_sendsSessionComplete() {
        FeedbackResponse feedback = new FeedbackResponse(UUID.randomUUID(), SESSION_ID, 80,
                null, null, null, null, null, null, "done", null, null);
        when(interviewSessionService.endSession(SESSION_ID, USER_ID)).thenReturn(feedback);

        controller.end(SESSION_ID, principal);

        assertThat(captureSent().type()).isEqualTo(WsOutgoingMessage.Type.SESSION_COMPLETE);
    }

    @Test
    void action_whenServiceThrows_sendsError() {
        when(interviewSessionService.startSession(SESSION_ID, USER_ID))
                .thenThrow(new IllegalStateException("boom"));

        controller.start(SESSION_ID, principal);

        assertThat(captureSent().type()).isEqualTo(WsOutgoingMessage.Type.ERROR);
    }

    @Test
    void start_nullPrincipal_doesNothing() {
        controller.start(SESSION_ID, null);
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    private WsOutgoingMessage captureSent() {
        ArgumentCaptor<WsOutgoingMessage> captor = ArgumentCaptor.forClass(WsOutgoingMessage.class);
        verify(messagingTemplate).convertAndSendToUser(eq(USER_ID.toString()), anyString(), captor.capture());
        return captor.getValue();
    }
}
