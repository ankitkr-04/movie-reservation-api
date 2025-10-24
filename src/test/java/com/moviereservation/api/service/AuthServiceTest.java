package com.moviereservation.api.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.moviereservation.api.domain.entities.User;
import com.moviereservation.api.domain.enums.UserRole;
import com.moviereservation.api.exception.EmailAlreadyExistsException;
import com.moviereservation.api.exception.InvalidCredentialsException;
import com.moviereservation.api.exception.PhoneAlreadyExistsException;
import com.moviereservation.api.security.JwtTokenProvider;
import com.moviereservation.api.web.dto.request.user.CreateUserRequest;
import com.moviereservation.api.web.dto.request.user.LoginRequest;
import com.moviereservation.api.web.dto.response.user.AuthResponse;
import com.moviereservation.api.web.mapper.UserMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private CreateUserRequest createUserRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setPhone("+1234567890");
        testUser.setPasswordHash("$2a$12$hashedPassword");
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setCreatedAt(Instant.now());
        testUser.setUpdatedAt(Instant.now());

        createUserRequest = new CreateUserRequest();
        createUserRequest.setEmail("test@example.com");
        createUserRequest.setPassword("SecurePass123!");
        createUserRequest.setFullName("Test User");
        createUserRequest.setPhoneNumber("+1234567890");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("SecurePass123!");
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUserSuccessfully() {
        // Arrange
        when(userService.existsByEmail(createUserRequest.getEmail())).thenReturn(false);
        when(userService.existsByPhone(createUserRequest.getPhoneNumber())).thenReturn(false);
        when(passwordEncoder.encode(createUserRequest.getPassword())).thenReturn("$2a$12$hashedPassword");
        when(userService.createCustomer(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(testUser);

        // Act
        User result = authService.register(createUserRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(createUserRequest.getEmail());
        assertThat(result.getRole()).isEqualTo(UserRole.CUSTOMER);

        verify(userService).existsByEmail(createUserRequest.getEmail());
        verify(userService).existsByPhone(createUserRequest.getPhoneNumber());
        verify(passwordEncoder).encode(createUserRequest.getPassword());
        verify(userService).createCustomer(
                createUserRequest.getFullName(),
                createUserRequest.getEmail(),
                createUserRequest.getPhoneNumber(),
                "$2a$12$hashedPassword"
        );
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Arrange
        when(userService.existsByEmail(createUserRequest.getEmail())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(createUserRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("already exists");

        verify(userService).existsByEmail(createUserRequest.getEmail());
        verify(userService, never()).createCustomer(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when phone already exists")
    void shouldThrowExceptionWhenPhoneAlreadyExists() {
        // Arrange
        when(userService.existsByEmail(createUserRequest.getEmail())).thenReturn(false);
        when(userService.existsByPhone(createUserRequest.getPhoneNumber())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(createUserRequest))
                .isInstanceOf(PhoneAlreadyExistsException.class)
                .hasMessageContaining("already exists");

        verify(userService).existsByEmail(createUserRequest.getEmail());
        verify(userService).existsByPhone(createUserRequest.getPhoneNumber());
        verify(userService, never()).createCustomer(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfullyWithValidCredentials() {
        // Arrange
        String token = "jwt.token.here";
        Long expiresIn = 86400000L;

        when(userService.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), testUser.getRole()))
                .thenReturn(token);
        when(jwtTokenProvider.getExpiryDuration()).thenReturn(expiresIn);
        when(userMapper.toResponse(testUser)).thenReturn(null); // UserResponse would be here

        // Act
        AuthResponse result = authService.login(loginRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(token);
        assertThat(result.getExpiresIn()).isEqualTo(expiresIn);

        verify(userService).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPasswordHash());
        verify(jwtTokenProvider).generateToken(testUser.getId(), testUser.getEmail(), testUser.getRole());
    }

    @Test
    @DisplayName("Should throw exception when user not found during login")
    void shouldThrowExceptionWhenUserNotFoundDuringLogin() {
        // Arrange
        when(userService.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userService).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when password is incorrect")
    void shouldThrowExceptionWhenPasswordIsIncorrect() {
        // Arrange
        when(userService.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPasswordHash())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userService).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPasswordHash());
        verify(jwtTokenProvider, never()).generateToken(any(), any(), any());
    }
}
