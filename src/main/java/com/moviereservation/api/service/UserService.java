package com.moviereservation.api.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviereservation.api.domain.entities.User;
import com.moviereservation.api.domain.enums.UserRole;
import com.moviereservation.api.exception.UserNotFoundException;
import com.moviereservation.api.repository.UserRepository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    /**
     * Create a new Customer user.
     * 
     * @param fullName
     * @param email
     * @param phone
     * @param passwordHash
     * @return Created User entity
     */
    public User createCustomer(final String fullName, final String email, final String phone,
            final String passwordHash) {
        return createUser(fullName, email, phone, passwordHash, UserRole.CUSTOMER);
    }

    /**
     * Create a new Admin user.
     * 
     * @param fullName
     * @param email
     * @param phone
     * @param passwordHash
     * @return Created User entity
     */
    public User createAdmin(final String fullName, final String email, final String phone, final String passwordHash) {
        return createUser(fullName, email, phone, passwordHash, UserRole.ADMIN);
    }

    /**
     * Create a new user with specified role.
     * 
     * @param fullName
     * @param email
     * @param phone
     * @param passwordHash
     * @param role
     * @return Created User entity
     */
    @Transactional
    public User createUser(final String fullName, final String email, final String phone, final String passwordHash,
            final UserRole role) {
        final User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordHash);
        user.setRole(role);

        final User savedUser = userRepository.save(user);
        log.info("User created: {} with role: {}", savedUser.getEmail(), role);

        return savedUser;
    }

    /**
     * Find user by ID.
     * 
     * @param userId
     * 
     * @throws UserNotFoundException if user not found
     * @return User entity
     */
    public User findById(final @NonNull UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
    }

    /**
     * Find user by email.
     * 
     * @param email
     * @return Optional User entity
     */
    public Optional<User> findByEmail(final @NonNull String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Check if a user exists with the given email.
     * 
     * @param email
     * @return true if user exists, false otherwise
     */
    public boolean existsByEmail(final @NonNull String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Check if a user exists with the given phone number.
     * 
     * @param phone
     * @return true if user exists, false otherwise
     */
    public boolean existsByPhone(final @NonNull String phone) {
        return userRepository.existsByPhone(phone);
    }

    /**
     * Promote a user to ADMIN role.
     * 
     * @param userId User ID to promote
     * @return Updated User entity
     * @throws UserNotFoundException if user not found
     * @throws IllegalStateException if user is already an ADMIN
     */
    @Transactional
    public User promoteToAdmin(final @NonNull UUID userId) {
        final User user = findById(userId);

        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalStateException("User is already an ADMIN");
        }

        user.setRole(UserRole.ADMIN);
        final User promotedUser = userRepository.save(user);

        log.info("User promoted to ADMIN: {}", user.getEmail());
        return promotedUser;
    }

    /**
     * Find all users with pagination.
     * 
     * @param pageable Pagination parameters
     * @return Page of User entities
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<User> findAll(
            final @NonNull org.springframework.data.domain.Pageable pageable) {
        return userRepository.findAll(pageable);
    }
}
