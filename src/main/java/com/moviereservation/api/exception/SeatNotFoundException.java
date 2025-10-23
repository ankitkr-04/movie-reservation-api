package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

public class SeatNotFoundException  extends BusinessException {
    public SeatNotFoundException(final String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    
}