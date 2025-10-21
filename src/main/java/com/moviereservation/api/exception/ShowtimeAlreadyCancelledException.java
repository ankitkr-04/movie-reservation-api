package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

public class ShowtimeAlreadyCancelledException extends BusinessException {

    public ShowtimeAlreadyCancelledException(final String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

}
