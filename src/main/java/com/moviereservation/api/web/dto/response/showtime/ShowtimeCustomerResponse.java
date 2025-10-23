package com.moviereservation.api.web.dto.response.showtime;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Showtime response DTO for customer-facing endpoints.
 * Excludes administrative metadata like createdAt/updatedAt and status.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeCustomerResponse {

    private UUID id;
    private MovieInfo movie;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant endTime;

    private Short screenNumber;
    private BigDecimal basePrice;
    private Short availableSeatsCount;

    /**
     * Simplified movie info for customer view.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovieInfo {
        private UUID id;
        private String title;
        private String description;
        private Integer duration;
        private String genre;
        private String rating;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Instant releaseDate;

        private String posterUrl;
    }
}