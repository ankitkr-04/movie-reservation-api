package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

public class ReservationNotFoundException extends BusinessException {
    public ReservationNotFoundException(final String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

}
