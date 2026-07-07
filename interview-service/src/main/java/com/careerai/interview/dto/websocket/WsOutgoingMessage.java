package com.careerai.interview.dto.websocket;

import java.time.LocalDateTime;

/**
 * A message pushed by the server to a subscribed client during an interview session.
 */
public record WsOutgoingMessage(
        Type type,
        Object payload,
        LocalDateTime timestamp
) {
    public enum Type {
        QUESTION,
        FEEDBACK,
        HINT,
        SESSION_COMPLETE,
        ERROR
    }

    public static WsOutgoingMessage of(Type type, Object payload) {
        return new WsOutgoingMessage(type, payload, LocalDateTime.now());
    }
}
