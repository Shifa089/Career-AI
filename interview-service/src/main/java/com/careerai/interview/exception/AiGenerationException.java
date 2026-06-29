package com.careerai.interview.exception;

/**
 * Thrown when Claude returns an empty or malformed response that cannot be parsed into the
 * expected structure. Handled as a generic 500 by the shared {@code GlobalExceptionHandler}.
 */
public class AiGenerationException extends RuntimeException {

    public AiGenerationException(String message) {
        super(message);
    }

    public AiGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
