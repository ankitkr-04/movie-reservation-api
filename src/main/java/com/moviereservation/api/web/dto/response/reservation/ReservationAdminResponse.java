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
 * Reservation response DTO for admin endpoints.
 * Includes full administrative metadata and user information.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationAdminResponse {

    private UUID id;
    private String bookingReference;
    private ReservationStatus status;
    private BigDecimal totalPrice;

    private UserInfo user;
    private ShowtimeInfo showtime;
    private List<BookedSeatDetails> seats;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private UUID id;
        private String fullName;
        private String email;
        private String phone;
    }

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
        private BigDecimal basePrice;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookedSeatDetails {
        private UUID seatInstanceId;
        private Character rowLabel;
        private Short seatNumber;
        private SeatType type; // REGULAR, PREMIUM
        private BigDecimal pricePaid;
    }
}