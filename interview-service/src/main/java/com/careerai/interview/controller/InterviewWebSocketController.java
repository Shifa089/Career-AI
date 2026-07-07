package com.careerai.interview.controller;

import com.careerai.interview.dto.request.SubmitAnswerRequest;
import com.careerai.interview.dto.response.QuestionResponse;
import com.careerai.interview.dto.websocket.WsIncomingMessage;
import com.careerai.interview.dto.websocket.WsOutgoingMessage;
import com.careerai.interview.service.InterviewSessionService;
import com.careerai.interview.service.SubmitAnswerResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * Real-time interview flow over STOMP. Clients connect to {@code /ws}, subscribe to
 * {@code /user/queue/interview/{sessionId}}, and send frames to {@code /app/interview/{sessionId}/*}.
 * The authenticated user id is resolved from the {@link Principal} set during the handshake.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class InterviewWebSocketController {

    private final InterviewSessionService interviewSessionService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/interview/{sessionId}/start")
    public void start(@DestinationVariable UUID sessionId, Principal principal) {
        dispatch(sessionId, principal, userId -> {
            QuestionResponse question = interviewSessionService.startSession(sessionId, userId);
            send(principal, sessionId, WsOutgoingMessage.Type.QUESTION, question);
        });
    }

    @MessageMapping("/interview/{sessionId}/answer")
    public void answer(@DestinationVariable UUID sessionId,
                       @Payload WsIncomingMessage message,
                       Principal principal) {
        dispatch(sessionId, principal, userId -> {
            UUID questionId = UUID.fromString(requireMetadata(message, "questionId"));
            SubmitAnswerResult result = interviewSessionService.submitAnswer(
                    sessionId, new SubmitAnswerRequest(questionId, message.content()), userId);

            // Per-answer scores are computed and persisted server-side but intentionally NOT
            // streamed back mid-interview. The candidate only sees a single consolidated report
            // once every question has been answered (SESSION_COMPLETE).
            if (result.completed()) {
                send(principal, sessionId, WsOutgoingMessage.Type.SESSION_COMPLETE, result.finalFeedback());
            } else {
                send(principal, sessionId, WsOutgoingMessage.Type.QUESTION, result.nextQuestion());
            }
        });
    }

    @MessageMapping("/interview/{sessionId}/hint")
    public void hint(@DestinationVariable UUID sessionId,
                     @Payload WsIncomingMessage message,
                     Principal principal) {
        dispatch(sessionId, principal, userId -> {
            String hint = interviewSessionService.generateHint(sessionId, userId, message.content());
            send(principal, sessionId, WsOutgoingMessage.Type.HINT, Map.of("hint", hint));
        });
    }

    @MessageMapping("/interview/{sessionId}/end")
    public void end(@DestinationVariable UUID sessionId, Principal principal) {
        dispatch(sessionId, principal, userId ->
                send(principal, sessionId, WsOutgoingMessage.Type.SESSION_COMPLETE,
                        interviewSessionService.endSession(sessionId, userId)));
    }

    private void dispatch(UUID sessionId, Principal principal, SessionAction action) {
        if (principal == null) {
            log.warn("Unauthenticated STOMP frame for session {}", sessionId);
            return;
        }
        try {
            action.run(UUID.fromString(principal.getName()));
        } catch (Exception e) {
            log.error("WebSocket interview action failed for session {}: {}", sessionId, e.getMessage());
            send(principal, sessionId, WsOutgoingMessage.Type.ERROR, Map.of("message", safeMessage(e)));
        }
    }

    private void send(Principal principal, UUID sessionId, WsOutgoingMessage.Type type, Object payload) {
        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/interview/" + sessionId,
                WsOutgoingMessage.of(type, payload));
    }

    private String requireMetadata(WsIncomingMessage message, String key) {
        if (message.metadata() == null || !message.metadata().containsKey(key)) {
            throw new IllegalArgumentException("Missing required metadata: " + key);
        }
        return message.metadata().get(key);
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null ? "Unexpected error" : e.getMessage();
    }

    @FunctionalInterface
    private interface SessionAction {
        void run(UUID userId);
    }
}
