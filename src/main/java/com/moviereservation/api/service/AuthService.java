package com.moviereservation.api.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviereservation.api.domain.entities.User;
import com.moviereservation.api.exception.EmailAlreadyExistsException;
import com.moviereservation.api.exception.InvalidCredentialsException;
import com.moviereservation.api.exception.PhoneAlreadyExistsException;
import com.moviereservation.api.security.JwtTokenProvider;
import com.moviereservation.api.web.dto.request.user.CreateUserRequest;
import com.moviereservation.api.web.dto.request.user.LoginRequest;
import com.moviereservation.api.web.dto.response.user.AuthResponse;
import com.moviereservation.api.web.mapper.UserMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for authentication operations: registration and login.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    /**
     * Register User registration details
     * 
     * @param request Registration request DTO
     * @throws EmailAlreadyExistsException if email is already taken
     * @throws PhoneAlreadyExistsException if phone number is already taken
     * @return Created User entity
     */
    @Transactional
    public User register(final CreateUserRequest request) {

        log.debug("Register new user with email: {}", request.getEmail());

        validateEmailAvailable(request.getEmail());
        validatePhoneAvailable(request.getPhoneNumber());
        // Hash the password
        final String passwordHash = passwordEncoder.encode(request.getPassword());

        // Create the new user
        final User user = userService.createCustomer(
                request.getFullName(),
                request.getEmail(),
                request.getPhoneNumber(),
                passwordHash);

        log.info("User registered successfully: {}", user.getEmail());
        return user;
    }

    /**
     * User login with credentials
     * 
     * @param request Login request DTO
     * @return AuthResponse containing JWT token and user info
     * @throws InvalidCredentialsException if credentials are invalid
     */
    @Transactional(readOnly = true)
    public AuthResponse login(final LoginRequest request) {
        log.debug("User login attempt with email: {}", request.getEmail());

        final User user = userService.findByEmail(request.getEmail()).orElseThrow(() -> {
            log.warn("Login failed: User not found with email: {}", request.getEmail());
            return new InvalidCredentialsException();
        });

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: Invalid password for email: {}", request.getEmail());
            throw new InvalidCredentialsException();
        }

        // Generate JWT token
        final String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole());

        final Long expiresIn = jwtTokenProvider.getExpiryDuration();

        log.info("User logged in successfully: {}", user.getEmail());
        return AuthResponse.builder()
                .user(userMapper.toResponse(user))
                .accessToken(token)
                .expiresIn(expiresIn)
                .build();
    }

    private void validateEmailAvailable(final String email) {
        if (userService.existsByEmail(email)) {
            log.warn("Registration failed: Email already exists: {}", email);
            throw new EmailAlreadyExistsException(email);
        }
    }

    private void validatePhoneAvailable(final String phone) {
        if (userService.existsByPhone(phone)) {
            log.warn("Registration failed: Phone already exists: {}", phone);
            throw new PhoneAlreadyExistsException(phone);
        }
    }

}
