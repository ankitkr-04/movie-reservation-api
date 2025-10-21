package com.moviereservation.api.web.dto.response.user;

import java.util.UUID;

import com.moviereservation.api.domain.enums.UserRole;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserResponse {
    private UUID id;
    private String fullName;
    private String email;

    private String phone;
    private UserRole role;

}
