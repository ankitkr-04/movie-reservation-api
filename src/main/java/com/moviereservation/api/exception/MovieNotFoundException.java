package com.moviereservation.api.exception;

import org.springframework.http.HttpStatus;

public class MovieNotFoundException extends BusinessException {
    public MovieNotFoundException(final String movieId) {
        super("Movie not found with id:" + movieId, HttpStatus.NOT_FOUND);
    }

    public MovieNotFoundException() {
        super("Movie not found", HttpStatus.NOT_FOUND);
    }

}
