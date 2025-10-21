package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

public class ShowtimeCancellationException extends BusinessException {

    public ShowtimeCancellationException(final String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

}
