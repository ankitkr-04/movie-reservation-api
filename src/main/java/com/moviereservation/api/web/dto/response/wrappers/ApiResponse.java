package com.moviereservation.api.web.dto.response.wrappers;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private List<String> errors;
    private Instant timestamp;

    // Success with data
    public static <T> ApiResponse<T> success(final String message, final T data) {
        final ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.message = message;
        response.data = data;
        response.timestamp = Instant.now();
        return response;
    }

    public static <T> ApiResponse<T> success(final String message) {
        return success(message, null);
    }

    public static <T> ApiResponse<T> success(final T data) {
        return success("Request successful", data);
    }

    public static <T> ApiResponse<T> success() {
        return success("Request successful", null);
    }

    // Error with multiple messages
    public static <T> ApiResponse<T> error(final String message, final List<String> errors) {
        final ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        response.errors = errors;
        response.timestamp = Instant.now();
        return response;
    }

    // Error with single message
    public static <T> ApiResponse<T> error(final String message, final String error) {
        return error(message, List.of(error));
    }

    // Error with just message (no error list)
    public static <T> ApiResponse<T> error(final String message) {
        final ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        // errors stays null
        response.timestamp = Instant.now();
        return response;
    }

    public static <T> ApiResponse<T> error() {
        return error("An error occurred");
    }
}
