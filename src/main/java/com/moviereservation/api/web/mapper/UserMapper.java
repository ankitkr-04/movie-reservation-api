package com.moviereservation.api.web.mapper;

import com.moviereservation.api.domain.entities.User;
import com.moviereservation.api.web.dto.response.user.UserResponse;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UserMapper {
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
