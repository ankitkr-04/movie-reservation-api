package com.moviereservation.api.web.dto.response.movie;

import java.time.Instant;
import java.util.UUID;

import com.moviereservation.api.domain.enums.Genre;
import com.moviereservation.api.domain.enums.MovieStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {

    private UUID id;
    private String title;
    private String description;
    private Integer duration;
    private Genre genre;
    private Instant releaseDate;
    private String posterUrl;
    private String rating;
    private MovieStatus status;

    //additional metadata
    private Instant createdAt;
    private Instant updatedAt;
}