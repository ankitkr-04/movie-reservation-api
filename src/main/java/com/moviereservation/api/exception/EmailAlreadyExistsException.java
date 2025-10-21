package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

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
