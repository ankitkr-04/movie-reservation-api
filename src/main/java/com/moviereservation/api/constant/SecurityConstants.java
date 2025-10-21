package com.moviereservation.api.constant;

import lombok.experimental.UtilityClass;

/**
 * Security-related constants used throughout the application.
 */
@UtilityClass
public final class SecurityConstants {

    // Role prefixes
    public static final String ROLE_PREFIX = "ROLE_";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    // JWT and authentication headers
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTH_HEADER = "Authorization";

    // Token expiration (in milliseconds)
    public static final long JWT_EXPIRATION_MS = 86400000L; // 24 hours
}
