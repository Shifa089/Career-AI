package com.careerai.auth.exception;

import com.careerai.common.dto.ApiResponse;
import com.careerai.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Auth-service-local advice that extends the shared
 * {@link com.careerai.common.exception.GlobalExceptionHandler} with handlers for
 * auth-specific exceptions. Ordered ahead of the shared (default-ordered) advice so
 * these take precedence; everything else falls through to the global handler.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserExists(UserAlreadyExistsException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", ex.getMessage(), request);
    }

    @ExceptionHandler(TokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleToken(TokenException ex,
                                                         HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", ex.getMessage(), request);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(AuthException ex,
                                                       HttpServletRequest request) {
        return build(ex.getStatus(), "AUTH_ERROR", ex.getMessage(), request);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UsernameNotFoundException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex,
                                                                 HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid email or password", request);
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String code,
                                                    String message, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(status.value(), code, message, request.getRequestURI());
        return ResponseEntity.status(status).body(ApiResponse.error(error));
    }
}
