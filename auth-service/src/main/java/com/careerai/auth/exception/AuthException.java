package com.careerai.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * General authentication/authorization failure that carries the HTTP status to
 * surface to the caller.
 */
public class AuthException extends RuntimeException {

    private final HttpStatus status;

    public AuthException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public AuthException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
