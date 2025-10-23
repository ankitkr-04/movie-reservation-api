package com.moviereservation.api.repository.specification;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.moviereservation.api.domain.entities.SeatInstance;
import com.moviereservation.api.domain.enums.SeatStatus;
import com.moviereservation.api.web.dto.request.seat.SeatMapFilterRequest;

/**
 * JPA Specifications for SeatInstance entity filtering.
 * Handles seat availability queries for specific showtimes.
 */
public class SeatSpecification {

    /**
     * Base filter for a specific showtime.
     * This is the foundation for all seat queries.
     */
    public static Specification<SeatInstance> forShowtime(final UUID showtimeId) {
        return (root, _, cb) -> cb.equal(root.get("showtime").get("id"), showtimeId);
    }

    /**
     * Soft-delete filter.
     */
    public static Specification<SeatInstance> isNotDeleted() {
        return (root, _, cb) -> cb.isNull(root.get("deletedAt"));
    }

    /**
     * Filter by row labels.
     */
    public static Specification<SeatInstance> hasRowLabelIn(final List<Character> rowLabels) {
        return (root, _, _) -> root.get("rowLabel").in(rowLabels);
    }

    /**
     * Filter by seat status.
     */
    public static Specification<SeatInstance> hasStatus(final SeatStatus status) {
        return (root, _, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Filter by multiple statuses.
     */
    public static Specification<SeatInstance> hasStatusIn(final List<SeatStatus> statuses) {
        return (root, _, _) -> root.get("status").in(statuses);
    }

    /**
     * Convenience methods for specific statuses.
     */
    public static Specification<SeatInstance> isAvailable() {
        return hasStatus(SeatStatus.AVAILABLE);
    }

    public static Specification<SeatInstance> isHeld() {
        return hasStatus(SeatStatus.HELD);
    }

    public static Specification<SeatInstance> isReserved() {
        return hasStatus(SeatStatus.RESERVED);
    }

    /**
     * Filter seats that are held by a specific user.
     */
    public static Specification<SeatInstance> isHeldBy(final UUID userId) {
        return (root, _, cb) -> cb.and(
                cb.equal(root.get("status"), SeatStatus.HELD),
                cb.equal(root.get("heldBy").get("id"), userId));
    }

    /**
     * Apply filters from SeatMapFilterRequest.
     * Combines showtime filter with optional row filters.
     */
    public static Specification<SeatInstance> withFilters(
            final UUID showtimeId,
            final SeatMapFilterRequest filters) {
        Specification<SeatInstance> spec = isNotDeleted()
                .and(forShowtime(showtimeId));

        // Filter by row labels if provided
        if (filters != null && !filters.getRowLabelsAsList().isEmpty()) {
            spec = spec.and(hasRowLabelIn(filters.getRowLabelsAsList()));
        }

        return spec;
    }

    /**
     * Get all seats for a showtime ordered by row and seat number.
     * This is the most common query for seat maps.
     */
    public static Specification<SeatInstance> forSeatMap(final UUID showtimeId) {
        return (root, query, cb) -> {
            // Apply base filters
            final var predicate = isNotDeleted()
                    .and(forShowtime(showtimeId))
                    .toPredicate(root, query, cb);

            // Add ordering
            if (query != null) {
                query.orderBy(
                        cb.asc(root.get("rowLabel")),
                        cb.asc(root.get("seatNumber")));
            }

            return predicate;
        };
    }

    /**
     * Get specific seats by IDs with pessimistic lock.
     * CRITICAL for reservation flow to prevent double-booking.
     * Lock must be applied at repository level, this just filters.
     */
    public static Specification<SeatInstance> byIds(final List<UUID> seatIds) {
        return (root, _, _) -> root.get("id").in(seatIds);
    }

    /**
     * Find expired holds that need to be released.
     * Used by background job to auto-release held seats.
     */
    public static Specification<SeatInstance> hasExpiredHold(final long holdDurationMinutes) {
        return (root, _, cb) -> {
            final var now = java.time.Instant.now();
            final var expiryTime = now.minusSeconds(holdDurationMinutes * 60);

            return cb.and(
                    cb.equal(root.get("status"), SeatStatus.HELD),
                    cb.isNotNull(root.get("heldAt")),
                    cb.lessThan(root.get("heldAt"), expiryTime));
        };
    }
}