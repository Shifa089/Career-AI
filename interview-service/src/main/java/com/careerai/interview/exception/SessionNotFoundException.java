package com.careerai.interview.exception;

import com.careerai.common.exception.ResourceNotFoundException;

import java.util.UUID;

/**
 * Thrown when an interview session cannot be found (or is not owned by the caller). Maps to 404
 * via the shared {@code GlobalExceptionHandler}.
 */
public class SessionNotFoundException extends ResourceNotFoundException {

    public SessionNotFoundException(UUID sessionId) {
        super("Interview session", sessionId);
    }
}
