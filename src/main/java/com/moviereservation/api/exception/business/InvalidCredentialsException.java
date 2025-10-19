package com.moviereservation.api.exception.business;

import org.springframework.http.HttpStatus;

import com.moviereservation.api.exception.BusinessException;

public class InvalidCredentialsException extends BusinessException {
    public InvalidCredentialsException() {
        super("Invalid credentials provided",
                HttpStatus.UNAUTHORIZED);
    }

    

}
