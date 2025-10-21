package com.moviereservation.api.web.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private UserResponse user;
    private String accessToken;
    private Long expiresIn;

    @Builder.Default
    private final String tokenType = "Bearer";
}
