package com.moviereservation.api.web.dto.response.seat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Seat map response organized by rows.
 * Changed from Map to List of Row objects for better structure.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatMapResponse {

        private UUID showtimeId;
        private Short screenNumber;
        private BigDecimal showtimeBasePrice;
        private List<SeatRow> rows;

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SeatRow {
                private Character rowLabel;
                private List<SeatAvailabilityResponse> seats;
        }
}