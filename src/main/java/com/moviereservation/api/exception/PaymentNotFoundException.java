package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

public class PaymentNotFoundException extends BusinessException {
    public PaymentNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

}
