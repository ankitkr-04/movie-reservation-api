package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedReservationAccessException extends BusinessException {
    public UnauthorizedReservationAccessException(final String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
