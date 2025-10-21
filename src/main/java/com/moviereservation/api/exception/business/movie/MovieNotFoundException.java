package com.moviereservation.api.exception.business.movie;

import org.springframework.http.HttpStatus;

import com.moviereservation.api.exception.BusinessException;

public class MovieNotFoundException extends BusinessException {
    public MovieNotFoundException(final String movieId) {
        super("Movie not found with id:"+ movieId, HttpStatus.NOT_FOUND);
    }

    public MovieNotFoundException() {
        super("Movie not found", HttpStatus.NOT_FOUND);
    }

}
