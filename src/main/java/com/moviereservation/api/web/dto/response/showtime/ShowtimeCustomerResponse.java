package com.moviereservation.api.web.dto.response.showtime;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.moviereservation.api.web.dto.response.movie.MovieCustomerResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeCustomerResponse {

    private UUID id;
    private MovieCustomerResponse movie;
    private Instant startTime;
    private Instant endTime;
    private Short screenNumber;
    private BigDecimal basePrice;
    private Short availableSeatsCount;
}