package com.moviereservation.api.web.dto.request.movie;

import java.time.Instant;

import org.hibernate.validator.constraints.URL;

import com.moviereservation.api.domain.enums.Genre;
import com.moviereservation.api.domain.enums.MovieStatus;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new movie.
 * Immutable - use builder pattern.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMovieRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be a positive number")
    @Max(value = 1000, message = "Duration cannot exceed 1000 minutes")
    private Integer duration;

    @NotNull(message = "Genre is required")
    private Genre genre;

    private MovieStatus status;

    @NotNull(message = "Release date is required")
    private Instant releaseDate;

    @URL(message = "Poster URL must be a valid URL")
    @Size(max = 500, message = "Poster URL cannot exceed 500 characters")
    private String posterUrl;

    @Size(max = 10, message = "Rating cannot exceed 10 characters")
    private String rating;
}
