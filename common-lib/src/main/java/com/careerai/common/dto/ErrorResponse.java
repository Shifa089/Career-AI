package com.careerai.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Structured error detail carried inside an {@link ApiResponse}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String code,
        String message,
        String path,
        List<FieldError> fieldErrors,
        Instant timestamp
) {

    public ErrorResponse(int status, String code, String message, String path) {
        this(status, code, message, path, null, Instant.now());
    }

    public record FieldError(String field, String message) {
    }
}
