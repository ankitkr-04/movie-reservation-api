package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

public class InvalidReservationException extends BusinessException {
    public InvalidReservationException(final String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

}
