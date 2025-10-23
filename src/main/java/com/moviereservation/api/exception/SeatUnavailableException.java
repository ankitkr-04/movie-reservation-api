package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

public class SeatUnavailableException extends BusinessException {
    public SeatUnavailableException(final String message) {
        super(message, HttpStatus.CONFLICT);
    }

}