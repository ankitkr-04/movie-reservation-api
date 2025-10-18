package com.moviereservation.api.domain.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservation_seats")
@Data
public class ReservationSeat {

    @EmbeddedId
    private ReservationSeatId id = new ReservationSeatId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("reservationId")
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("seatInstanceId")
    @JoinColumn(name = "seat_instance_id", nullable = false)
    private SeatInstance seatInstance;

    @Column(name = "price_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePaid;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationSeatId implements Serializable {
        private UUID reservationId;
        private UUID seatInstanceId;

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ReservationSeatId that = (ReservationSeatId) o;
            return Objects.equals(reservationId, that.reservationId) &&
                    Objects.equals(seatInstanceId, that.seatInstanceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(reservationId, seatInstanceId);
        }
    }
}