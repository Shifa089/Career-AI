package com.careerai.common.exception;

/**
 * Thrown when the client submits an invalid request. Maps to HTTP 400.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
