package com.careerai.auth.exception;

/**
 * Raised when registering an email that already has an account. Rendered as HTTP 409.
 */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
