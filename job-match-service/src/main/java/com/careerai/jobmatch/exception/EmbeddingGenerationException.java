package com.careerai.jobmatch.exception;

/**
 * Raised when an embedding could not be generated (e.g. the embedding model call failed). Surfaced
 * as HTTP 502 by {@link JobMatchExceptionHandler}.
 */
public class EmbeddingGenerationException extends RuntimeException {

    public EmbeddingGenerationException(String message) {
        super(message);
    }

    public EmbeddingGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
