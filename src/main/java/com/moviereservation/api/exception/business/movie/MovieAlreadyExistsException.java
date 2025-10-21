package com.moviereservation.api.exception.business.movie;

import org.springframework.http.HttpStatus;

import com.moviereservation.api.exception.BusinessException;

public class MovieAlreadyExistsException extends BusinessException {
    public MovieAlreadyExistsException(final String title) {
        super("Movie already exists with title: " + title,
                HttpStatus.CONFLICT);
    }

    public MovieAlreadyExistsException() {
        super("Movie already exists",
                HttpStatus.CONFLICT);
    }

}
