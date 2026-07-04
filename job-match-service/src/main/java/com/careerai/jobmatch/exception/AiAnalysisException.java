package com.careerai.jobmatch.exception;

/**
 * Raised when a Claude-backed analysis call fails or returns unusable output. Surfaced as HTTP 502
 * by {@link JobMatchExceptionHandler}.
 */
public class AiAnalysisException extends RuntimeException {

    public AiAnalysisException(String message) {
        super(message);
    }

    public AiAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
