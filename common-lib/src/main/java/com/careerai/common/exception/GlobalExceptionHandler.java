package com.careerai.common.exception;

import com.careerai.common.dto.ApiResponse;
import com.careerai.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Centralized REST exception handler shared by all CareerAI services.
 * Every failure is rendered as an {@link ApiResponse} carrying an {@link ErrorResponse}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                "Request validation failed",
                request.getRequestURI(),
                fieldErrors,
                java.time.Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", request);
    }

    private ErrorResponse.FieldError toFieldError(FieldError fe) {
        return new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage());
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String code,
                                                    String message, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(status.value(), code, message, request.getRequestURI());
        return ResponseEntity.status(status).body(ApiResponse.error(error));
    }
}
