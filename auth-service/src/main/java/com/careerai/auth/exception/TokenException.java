package com.careerai.auth.exception;

/**
 * Raised when a JWT/refresh token is missing, malformed, expired, revoked, or of
 * the wrong type. Rendered as HTTP 401.
 */
public class TokenException extends RuntimeException {

    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
