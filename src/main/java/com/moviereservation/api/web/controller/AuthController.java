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
import com.moviereservation.api.web.dto.request.user.LoginRequest;
import com.moviereservation.api.web.dto.request.user.CreateUserRequest;
import com.moviereservation.api.web.dto.response.user.AuthResponse;
import com.moviereservation.api.web.dto.response.user.UserResponse;
import com.moviereservation.api.web.dto.response.wrappers.ApiResponse;
import com.moviereservation.api.web.mapper.UserMapper;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Authentication endpoints: registration and login.
 */
@RestController
@RequestMapping(Route.AUTH)
@Tag(name = "Authentication", description = "User registration and login")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final UserMapper userMapper; // Inject MapStruct mapper

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(
            @RequestBody @Valid final CreateUserRequest request) {
        
        final User user = authService.registerUser(request);
        final UserResponse userResponse = userMapper.toResponse(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("User registered successfully", userResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginUser(
            @RequestBody @Valid final LoginRequest request) {
        
        final AuthResponse authResponse = authService.loginUser(request);

        return ResponseEntity.ok(
                ApiResponse.success("User logged in successfully", authResponse));
    }
}

