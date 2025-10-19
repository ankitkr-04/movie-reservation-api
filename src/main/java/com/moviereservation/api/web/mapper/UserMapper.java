package com.moviereservation.api.web.mapper;

import com.moviereservation.api.domain.entities.User;
import com.moviereservation.api.web.dto.response.UserResponse;

public class UserMapper {
    private UserMapper() {
        // Private constructor prevents instantiation
        throw new UnsupportedOperationException("Utility class");
    }

    public static UserResponse toResponse(final User user) {

        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .build();
    }

}
