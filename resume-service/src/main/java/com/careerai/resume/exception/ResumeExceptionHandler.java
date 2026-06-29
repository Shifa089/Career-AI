package com.careerai.resume.exception;

import com.careerai.common.dto.ApiResponse;
import com.careerai.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Resume-service-specific advice. Only extends the shared {@code GlobalExceptionHandler} for
 * exceptions without a common-lib parent; everything else is handled centrally. Ordered ahead of
 * the shared (default-ordered) advice so these take precedence.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ResumeExceptionHandler {

    @ExceptionHandler(AiAnalysisException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiAnalysis(AiAnalysisException ex,
                                                              HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_GATEWAY.value(),
                "AI_ANALYSIS_FAILED",
                ex.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.error(error));
    }
}
