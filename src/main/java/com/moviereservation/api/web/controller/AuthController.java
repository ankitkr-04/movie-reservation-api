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
import com.moviereservation.api.web.dto.request.user.LoginUserRequest;
import com.moviereservation.api.web.dto.request.user.RegisterUserRequest;
import com.moviereservation.api.web.dto.response.user.AuthResponse;
import com.moviereservation.api.web.dto.response.user.UserResponse;
import com.moviereservation.api.web.dto.response.wrappers.ApiResponse;
import com.moviereservation.api.web.mapper.UserMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(Route.AUTH)
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(
            @RequestBody @Valid final RegisterUserRequest request) {
        // Implementation for user registration
        final User user = authService.registerUser(request);
        final UserResponse userResponse = UserMapper.toResponse(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("User registered Successfully", userResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginUser(@RequestBody @Valid final LoginUserRequest request) {
        final AuthResponse authResponse = authService.loginUser(request);

        return ResponseEntity.ok(
                ApiResponse.success("User logged in successfully", authResponse));
    }

}
