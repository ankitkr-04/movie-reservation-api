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

import com.moviereservation.api.constant.MovieConstants;
import com.moviereservation.api.domain.enums.ShowtimeStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "showtimes")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE showtimes SET deleted_at = CURRENT_TIMESTAMP WHERE showtime_id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@ToString(exclude = { "movie", "createdBy" }) // Avoid lazy loading and circular references
public class Showtime {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "showtime_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "screen_number", nullable = false)
    private Short screenNumber;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShowtimeStatus status = ShowtimeStatus.SCHEDULED;

    @Column(name = "available_seats_count", nullable = false)
    private Short availableSeatsCount = MovieConstants.DEFAULT_AVAILABLE_SEATS;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Showtime))
            return false;
        Showtime showtime = (Showtime) o;
        return id != null && Objects.equals(id, showtime.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
