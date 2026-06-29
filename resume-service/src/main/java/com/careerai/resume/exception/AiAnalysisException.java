package com.careerai.resume.exception;

/**
 * Thrown when the AI provider fails to produce a usable analysis (API error, malformed JSON,
 * or circuit open). Rendered as HTTP 502 by {@link ResumeExceptionHandler}.
 */
public class AiAnalysisException extends RuntimeException {

    public AiAnalysisException(String message) {
        super(message);
    }

    public AiAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
