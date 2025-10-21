package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

public class ShowtimeNotFoundException extends BusinessException {

    public ShowtimeNotFoundException(final String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

}
