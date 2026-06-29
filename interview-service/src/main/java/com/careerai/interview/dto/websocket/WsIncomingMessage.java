package com.careerai.interview.dto.websocket;

import java.util.Map;

/**
 * A message sent by the client over STOMP into an interview session.
 */
public record WsIncomingMessage(
        Type type,
        String content,
        Map<String, String> metadata
) {
    public enum Type {
        START_SESSION,
        SUBMIT_ANSWER,
        REQUEST_HINT,
        PAUSE,
        END_SESSION
    }
}
