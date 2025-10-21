package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

public class PhoneAlreadyExistsException extends BusinessException {
    public PhoneAlreadyExistsException(final String phone) {
        super("Phone Number already exists: " + phone,
                HttpStatus.CONFLICT);
    }

    public PhoneAlreadyExistsException() {
        super("Phone Number already exists",
                HttpStatus.CONFLICT);
    }

}
