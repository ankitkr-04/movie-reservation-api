package com.moviereservation.api.web.dto.request.movie;

import java.time.Instant;
import java.util.List;

import com.moviereservation.api.domain.enums.Genre;
import com.moviereservation.api.domain.enums.MovieStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Filter criteria for movie queries.
 * Immutable request DTO - use builder to create instances.
 */
@Getter
@Builder(toBuilder = true) // Enable toBuilder for creating modified copies
@NoArgsConstructor
@AllArgsConstructor
public class MovieFilterRequest {

    @Schema(description = "Case-insensitive substring search on title", example = "inception")
    private String title;

    @Schema(description = "Filter by one or more genres (OR semantics)", example = "ACTION,DRAMA")
    private List<Genre> genres;

    @Schema(description = "Filter by one or more statuses", example = "COMING_SOON,ACTIVE")
    private List<MovieStatus> statuses;

    @Schema(description = "Release date start (inclusive)", example = "2024-01-01T00:00:00Z")
    private Instant releaseDateFrom;

    @Schema(description = "Release date end (inclusive)", example = "2025-12-31T23:59:59Z")
    private Instant releaseDateTo;

    @Schema(description = "Minimum duration in minutes", example = "60")
    private Integer durationMin;

    @Schema(description = "Maximum duration in minutes", example = "180")
    private Integer durationMax;

    /**
     * Creates a copy of this filter without status filters.
     * Useful when applying role-based status restrictions.
     */
    public MovieFilterRequest withoutStatuses() {
        return this.toBuilder()
                .statuses(null)
                .build();
    }
}
