package com.careerai.interview.exception;

import com.careerai.common.exception.BadRequestException;

/**
 * Thrown when an operation requires a session in a particular state but the session has already
 * progressed past it (e.g. starting a session that is not {@code CREATED}). Maps to 400.
 */
public class SessionAlreadyActiveException extends BadRequestException {

    public SessionAlreadyActiveException(String message) {
        super(message);
    }
}
