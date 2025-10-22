package com.moviereservation.api.web.dto.response.seat;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Builder;

@Builder
public record SeatResponse(
                Short number,
                String status, // "A" (AVAILABLE), "H" (HELD), "R" (RESERVED)
                String type, // "R" (REGULAR), "P" (PREMIUM), "V" (VIP)
                BigDecimal totalPrice,
                Instant heldUntil // Only for HELD seats
) {
}