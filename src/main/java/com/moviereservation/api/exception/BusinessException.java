package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus status;

    public BusinessException(final String message, final HttpStatus status) {
        super(message);
        this.status = status;
    }

}
