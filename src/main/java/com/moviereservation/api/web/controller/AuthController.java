package com.moviereservation.api.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moviereservation.api.constant.Route;
import com.moviereservation.api.domain.entities.User;
import com.moviereservation.api.service.AuthService;
import com.moviereservation.api.web.dto.request.user.CreateUserRequest;
import com.moviereservation.api.web.dto.request.user.LoginRequest;
import com.moviereservation.api.web.dto.response.user.AuthResponse;
import com.moviereservation.api.web.dto.response.user.UserResponse;
import com.moviereservation.api.web.dto.response.wrappers.ApiResponse;
import com.moviereservation.api.web.mapper.UserMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Authentication endpoints for user registration and login.
 * Public access - no authentication required.
 */
@RestController
@RequestMapping(Route.AUTH)
@Tag(name = "Authentication", description = "User registration and login endpoints")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;

    /**
     * Register a new customer account.
     * Users are created with CUSTOMER role by default.
     */
    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Creates a new customer account. Email must be unique.")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody final CreateUserRequest request) {

        final User user = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", userMapper.toResponse(user)));
    }

    /**
     * Login with email and password.
     * Returns JWT token valid for 24 hours.
     */
    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates user and returns JWT token (24-hour expiry)")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody final LoginRequest request) {

        final AuthResponse auth = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success("Login successful", auth));
    }
}