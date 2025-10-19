package com.moviereservation.api.security;

import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;

import com.moviereservation.api.domain.enums.UserRole;

public interface UserPrincipal extends UserDetails {
    UUID getUserId();

    UserRole getRole();

    String getEmail();

}
