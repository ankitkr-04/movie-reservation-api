package com.moviereservation.api.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class MovieDeletionException extends BusinessException {

    public MovieDeletionException(final String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public MovieDeletionException(final UUID movieId) {
        super("Cannot delete movie with ID " + movieId + " due to active or upcoming reservations.",
                HttpStatus.BAD_REQUEST);
    }

    public MovieDeletionException() {
        super("Cannot delete movie with active or upcoming reservations.", HttpStatus.BAD_REQUEST);
    }

}
