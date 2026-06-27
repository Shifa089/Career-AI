package com.careerai.common.exception;

/**
 * Thrown when authentication or authorization fails. Maps to HTTP 401.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
