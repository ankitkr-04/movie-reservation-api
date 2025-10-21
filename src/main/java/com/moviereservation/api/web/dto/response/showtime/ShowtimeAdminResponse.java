package com.moviereservation.api.web.dto.response.showtime;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.moviereservation.api.domain.enums.ShowtimeStatus;
import com.moviereservation.api.web.dto.response.movie.MovieAdminResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeAdminResponse {

    private UUID id;
    private MovieAdminResponse movie;
    private Instant startTime;
    private Instant endTime;
    private Short screenNumber;
    private BigDecimal basePrice;
    private ShowtimeStatus status;
    private Short availableSeatsCount;
    private Instant createdAt;
    private Instant updatedAt;
}