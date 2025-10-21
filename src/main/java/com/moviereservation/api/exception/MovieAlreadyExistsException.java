package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

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
