package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

public class ShowtimeConflictException extends BusinessException {

    public ShowtimeConflictException(final String message) {
        super(message, HttpStatus.CONFLICT);
    }

}
