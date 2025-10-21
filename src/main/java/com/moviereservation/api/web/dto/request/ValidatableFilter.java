package com.moviereservation.api.web.dto.request;

import com.moviereservation.api.domain.enums.UserRole;

public interface ValidatableFilter {
    /**
     * Validate and sanitize filter based on user role.
     * 
     * @param role User's role (ADMIN, CUSTOMER, etc.)
     */
    void validateForRole(UserRole role);
}