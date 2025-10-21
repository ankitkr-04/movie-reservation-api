package com.moviereservation.api.web.dto.request.movie;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import com.moviereservation.api.domain.enums.Genre;
import com.moviereservation.api.domain.enums.MovieStatus;
import com.moviereservation.api.domain.enums.UserRole;
import com.moviereservation.api.web.dto.request.ValidatableFilter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterMovieRequest implements ValidatableFilter {

    @Schema(description = "Case-insensitive substring search on title", example = "inception")
    private String title;

    @Schema(description = "Filter by one or more genres (OR semantics)", example = "ACTION,DRAMA")
    private List<Genre> genres;

    @Schema(description = "Filter by one or more statuses (ADMIN only). Customers will be constrained to visible statuses.", example = "COMING_SOON,ACTIVE")
    private List<MovieStatus> statuses;

    @Schema(description = "Release date start (inclusive)", example = "2024-01-01T00:00:00Z")
    private Instant releaseDateFrom;

    @Schema(description = "Release date end (inclusive)", example = "2025-12-31T23:59:59Z")
    private Instant releaseDateTo;

    @Schema(description = "Minimum duration in minutes", example = "60")
    private Integer durationMin;

    @Schema(description = "Maximum duration in minutes", example = "180")
    private Integer durationMax;

    @Override
    public void validateForRole(final UserRole role) {
        if (role == UserRole.CUSTOMER && statuses != null && !statuses.isEmpty()) {
            // Strip out INACTIVE status for customers
            statuses = statuses.stream()
                    .filter(s -> s == MovieStatus.ACTIVE || s == MovieStatus.COMING_SOON)
                    .collect(Collectors.toList());
        }
    }
}
