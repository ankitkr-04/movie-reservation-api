package com.moviereservation.api.web.dto.response.movie;

import java.time.Instant;
import java.util.UUID;

import com.moviereservation.api.domain.enums.Genre;
import com.moviereservation.api.domain.enums.MovieStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Movie response DTO for customer-facing endpoints.
 * Excludes administrative metadata like createdAt/updatedAt.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieCustomerResponse {

    private UUID id;
    private String title;
    private String description;
    private Integer duration; // in minutes
    private Genre genre;
    private Instant releaseDate;
    private String posterUrl;
    private String rating;
    private MovieStatus status;
}
