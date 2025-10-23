package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

public class PaymentException extends BusinessException {
    public PaymentException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

}
