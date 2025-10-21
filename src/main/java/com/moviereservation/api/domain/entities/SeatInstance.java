package com.moviereservation.api.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.moviereservation.api.domain.enums.SeatStatus;
import com.moviereservation.api.domain.enums.SeatType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "seat_instance")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE seat_instance SET deleted_at = CURRENT_TIMESTAMP WHERE seat_instance_id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@ToString(exclude = { "showtime", "seatTemplate", "heldBy", "createdBy", "updatedBy" })
public class SeatInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "seat_instance_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_template_id", nullable = false)
    private SeatTemplate seatTemplate;

    @Column(name = "row_label", nullable = false, length = 1)
    private Character rowLabel;

    @Column(name = "seat_number", nullable = false)
    private Short seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private SeatType type;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Column(name = "held_at")
    private Instant heldAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "held_by")
    private User heldBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SeatInstance))
            return false;
        SeatInstance that = (SeatInstance) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
