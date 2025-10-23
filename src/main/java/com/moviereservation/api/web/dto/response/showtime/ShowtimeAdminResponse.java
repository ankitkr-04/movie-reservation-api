package com.moviereservation.api.web.dto.response.showtime;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moviereservation.api.domain.enums.ShowtimeStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Showtime response DTO for admin endpoints.
 * Includes full administrative metadata and complete movie details.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeAdminResponse {

    private UUID id;
    private MovieInfo movie;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant startTime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant endTime;
    
    private Short screenNumber;
    private BigDecimal basePrice;
    private ShowtimeStatus status;
    private Short availableSeatsCount;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedAt;
    
    /**
     * Nested movie info to avoid circular dependencies.
     * Contains essential movie details without full entity.
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
        private String status;
        
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Instant releaseDate;
        
        private String posterUrl;
    }
}