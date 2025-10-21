package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends BusinessException {
    public InvalidCredentialsException() {
        super("Invalid credentials provided",
                HttpStatus.UNAUTHORIZED);
    }

}
