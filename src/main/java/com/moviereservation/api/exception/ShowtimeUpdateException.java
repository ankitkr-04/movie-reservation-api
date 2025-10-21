package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

public class ShowtimeUpdateException extends BusinessException {

    public ShowtimeUpdateException(final String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

}
