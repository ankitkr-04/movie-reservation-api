package com.moviereservation.api.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.moviereservation.api.web.dto.response.ApiResponse;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // --- Business exceptions (all custom domain errors) ---
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(final BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage()); // WARN, not ERROR
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getMessage()));
    }

    // --- Validation errors ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(final MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage()); // WARN, expected error
        final var errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed", errors));
    }

    // --- Authentication / JWT errors ---
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(final AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage()); // WARN, could be normal failed login
        return ResponseEntity.status(401).body(ApiResponse.error("Authentication failed"));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwtException(final JwtException ex) { // Fixed type
        log.error("JWT exception: {}", ex.getMessage()); // ERROR, could be security issue
        return ResponseEntity.status(401).body(ApiResponse.error("Invalid or expired token"));
    }

    // --- Authorization / role violations ---
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(final AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage()); // WARN, expected for wrong roles
        return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
    }

    // --- Malformed JSON ---
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJson(final HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage()); // WARN, client error
        return ResponseEntity.badRequest().body(ApiResponse.error("Malformed JSON request"));
    }

    // --- Wrong HTTP method ---

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(final HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP method not supported: {}", ex.getMessage()); // Just log the message
        return ResponseEntity.status(405).body(ApiResponse.error("Method not supported: " + ex.getMethod()));
    }

    // --- Fallback for any other unexpected exceptions ---
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(final Exception ex) {
        log.error("Unexpected error occurred", ex); // ERROR with full stack trace
        return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
    }
}