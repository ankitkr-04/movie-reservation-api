package com.moviereservation.api.web.dto.response.seat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moviereservation.api.domain.enums.SeatStatus;
import com.moviereservation.api.domain.enums.SeatType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Seat availability response with full enum types instead of abbreviations.
 * Consistent with domain model.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatAvailabilityResponse {
    
    private UUID seatInstanceId;
    private Character rowLabel;
    private Short number;
    private SeatStatus status; // AVAILABLE, HELD, RESERVED
    private SeatType type; // REGULAR, PREMIUM
    private BigDecimal totalPrice;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant heldUntil; // Only for HELD seats
}