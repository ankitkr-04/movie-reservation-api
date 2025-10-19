package com.moviereservation.api.exception.business;

import org.springframework.http.HttpStatus;

import com.moviereservation.api.exception.BusinessException;

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException(final String identifier) {
        super("User not found: " + identifier,
                HttpStatus.NOT_FOUND);
    }

    public UserNotFoundException() {
        super("User not found",
                HttpStatus.NOT_FOUND);
    }
}
