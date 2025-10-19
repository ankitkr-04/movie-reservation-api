package com.moviereservation.api.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.moviereservation.api.domain.entities.User;
import com.moviereservation.api.domain.enums.UserRole;
import com.moviereservation.api.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User createNewUser(
            final String fullName,
            final String email,
            final String phoneNumber,
            final String passwordHash,
            final UserRole role) {

        // Create and save the new user
        final User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setPhone(phoneNumber);
        newUser.setPasswordHash(passwordHash);
        newUser.setRole(role);

        return userRepository.save(newUser);
    }

    public User createNewUser(
            final String fullName,
            final String email,
            final String phoneNumber,
            final String passwordHash) {
        return createNewUser(fullName, email, phoneNumber, passwordHash, UserRole.CUSTOMER);
    }

    public Optional<User> getUserByEmail(final String email) {
        return userRepository.findByEmail(email);
    }

    public boolean isEmailTaken(final String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean isPhoneTaken(final String phoneNumber) {
        return userRepository.existsByPhone(phoneNumber);
    }

}
