package com.careerai.jobmatch.exception;

import com.careerai.common.dto.ApiResponse;
import com.careerai.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Service-specific error handling that extends (does not replace) the shared
 * {@link com.careerai.common.exception.GlobalExceptionHandler}. Upstream AI/embedding failures are
 * reported as 502 Bad Gateway so callers can distinguish them from client errors.
 */
@RestControllerAdvice
@Slf4j
public class JobMatchExceptionHandler {

    @ExceptionHandler({AiAnalysisException.class, EmbeddingGenerationException.class})
    public ResponseEntity<ApiResponse<Void>> handleUpstreamAiFailure(RuntimeException ex,
                                                                      HttpServletRequest request) {
        log.error("Upstream AI/embedding failure on {}", request.getRequestURI(), ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_GATEWAY.value(), "AI_PROVIDER_ERROR", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.error(error));
    }
}
