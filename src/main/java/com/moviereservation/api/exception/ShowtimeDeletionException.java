package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

public class ShowtimeDeletionException extends BusinessException {

    public ShowtimeDeletionException(final String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

}
