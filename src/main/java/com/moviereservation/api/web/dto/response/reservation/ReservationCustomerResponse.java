package com.moviereservation.api.web.dto.response.reservation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moviereservation.api.domain.enums.ReservationStatus;
import com.moviereservation.api.domain.enums.SeatType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Reservation response DTO for customer-facing endpoints.
 * Excludes administrative metadata and sensitive user details.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCustomerResponse {
    
    private UUID id;
    private String bookingReference;
    private ReservationStatus status;
    private BigDecimal totalPrice;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
    
    private ShowtimeInfo showtime;
    private List<BookedSeatDetails> seats;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShowtimeInfo {
        private UUID id;
        private String movieTitle;
        
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Instant startTime;
        
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Instant endTime;
        
        private Short screenNumber;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookedSeatDetails {
        private UUID seatInstanceId;
        private Character rowLabel;
        private Short seatNumber;
        private SeatType type; 
        private BigDecimal pricePaid;
    }
}