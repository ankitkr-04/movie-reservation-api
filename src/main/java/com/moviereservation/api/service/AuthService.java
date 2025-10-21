package com.moviereservation.api.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviereservation.api.domain.entities.User;
import com.moviereservation.api.exception.business.EmailAlreadyExistsException;
import com.moviereservation.api.exception.business.InvalidCredentialsException;
import com.moviereservation.api.exception.business.PhoneAlreadyExistsException;
import com.moviereservation.api.security.JwtTokenProvider;
import com.moviereservation.api.web.dto.request.user.LoginUserRequest;
import com.moviereservation.api.web.dto.request.user.RegisterUserRequest;
import com.moviereservation.api.web.dto.response.user.AuthResponse;
import com.moviereservation.api.web.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @Transactional
    public User registerUser(final RegisterUserRequest request) {

        if (userService.isEmailTaken(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        if (userService.isPhoneTaken(request.getPhoneNumber())) {
            throw new PhoneAlreadyExistsException(request.getPhoneNumber());
        }

        // Hash the password
        final String passwordHash = passwordEncoder.encode(request.getPassword());

        // Create the new user
        final User newUser = userService.createNewUser(
                request.getFullName(),
                request.getEmail(),
                request.getPhoneNumber(),
                passwordHash);

        return newUser;
    }

    @Transactional(readOnly = true)
    public AuthResponse loginUser(final LoginUserRequest request) {
        final Optional<User> res = userService.getUserByEmail(request.getEmail());

        if (res.isEmpty()) {
            throw new InvalidCredentialsException();
        }

        final User user = res.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        final String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(),
                user.getRole());
        final Long expiresIn = jwtTokenProvider.getExpiryDuration();

        return AuthResponse.builder()
                .user(UserMapper.toResponse(user))
                .accessToken(token)
                .expiresIn(expiresIn)
                .build();
    }

}
