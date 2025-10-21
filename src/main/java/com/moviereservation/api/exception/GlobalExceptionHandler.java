package com.moviereservation.api.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.moviereservation.api.web.dto.response.wrappers.ApiResponse;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for all REST controllers.
 * Provides consistent error responses across the application.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // --- Business exceptions (all custom domain errors) ---
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(final BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getMessage()));
    }

    // --- Validation errors ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(final MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        final var errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed", errors));
    }

    // --- Type conversion errors (e.g., invalid UUID format) ---
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(final MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getMessage());

        String message = "Invalid value for parameter '" + ex.getName() + "'";
        final Class<?> requiredType = ex.getRequiredType();
        if (requiredType != null && "UUID".equals(requiredType.getSimpleName())) {
            message = "Invalid UUID format for parameter '" + ex.getName() + "'";
        }

        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    // --- IllegalArgumentException (general validation) ---
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(final IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    // --- Authentication / JWT errors ---
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(final AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(401).body(ApiResponse.error("Authentication failed"));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwtException(final JwtException ex) {
        log.error("JWT exception: {}", ex.getMessage());
        return ResponseEntity.status(401).body(ApiResponse.error("Invalid or expired token"));
    }

    // --- Authorization / role violations ---
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(final AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
    }

    // --- Malformed JSON ---
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJson(final HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error("Malformed JSON request"));
    }

    // --- Wrong HTTP method ---
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(final HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP method not supported: {}", ex.getMessage());
        return ResponseEntity.status(405).body(ApiResponse.error("Method not supported: " + ex.getMethod()));
    }

    // --- Fallback for any other unexpected exceptions ---
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(final Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
    }
}
