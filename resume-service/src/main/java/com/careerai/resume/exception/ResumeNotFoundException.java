package com.careerai.resume.exception;

import com.careerai.common.exception.ResourceNotFoundException;

import java.util.UUID;

/**
 * Thrown when a resume cannot be found (or is not visible to the requesting user). Handled
 * centrally as HTTP 404 via the shared {@code GlobalExceptionHandler}.
 */
public class ResumeNotFoundException extends ResourceNotFoundException {

    public ResumeNotFoundException(UUID resumeId) {
        super("Resume", resumeId);
    }
}
