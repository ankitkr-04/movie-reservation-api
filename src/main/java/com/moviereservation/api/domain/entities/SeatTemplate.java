package com.moviereservation.api.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.moviereservation.api.domain.enums.SeatType;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "seat_template")
@EntityListeners(AuditingEntityListener.class)
@Data
public class SeatTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "seat_template_id")
    private UUID id;

    @Column(name = "screen_number", nullable = false)
    private Short screenNumber;

    @Column(name = "row_label", nullable = false, length = 1)
    private Character rowLabel;

    @Column(name = "seat_number", nullable = false)
    private Short seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private SeatType type;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
