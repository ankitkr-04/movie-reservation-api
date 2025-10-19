package com.moviereservation.api.exception.business;

import org.springframework.http.HttpStatus;

import com.moviereservation.api.exception.BusinessException;

public class EmailAlreadyExistsException extends BusinessException {
    public EmailAlreadyExistsException(final String email) {
        super("Email already exists: " + email,
                HttpStatus.CONFLICT);
    }

    public EmailAlreadyExistsException() {
        super("Email already exists",
                HttpStatus.CONFLICT);
    }

}
