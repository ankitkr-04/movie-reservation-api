package com.moviereservation.api.security;

import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;

public interface UserPrincipal extends UserDetails {
    UUID getUserId();

}
