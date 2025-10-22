package com.moviereservation.api.web.dto.response.seat;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import lombok.Builder;

@Builder
public record SeatMapResponse(
        UUID showtimeId,
        Short screenNumber,
        BigDecimal showtimeBasePrice,
        Map<String, SeatResponse[]> rows) {
}