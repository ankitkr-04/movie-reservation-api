package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

public class InvalidReservationCancellationException extends BusinessException {
    public InvalidReservationCancellationException(final String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
    
}
