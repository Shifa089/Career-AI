package com.careerai.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Standard envelope wrapping every REST response across CareerAI services.
 *
 * @param <T> the payload type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        ErrorResponse error,
        Instant timestamp
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", data, null, Instant.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return new ApiResponse<>(false, error.message(), null, error, Instant.now());
    }
}
