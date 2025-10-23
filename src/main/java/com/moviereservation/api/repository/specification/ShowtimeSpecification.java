package com.moviereservation.api.repository.specification;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.moviereservation.api.domain.entities.Showtime;
import com.moviereservation.api.domain.enums.ShowtimeStatus;
import com.moviereservation.api.web.dto.request.showtime.ShowtimeFilterRequest;

import jakarta.persistence.criteria.JoinType;

/**
 * JPA Specifications for Showtime entity filtering.
 * Handles both admin (all showtimes) and customer (future scheduled only)
 * contexts.
 */
public class ShowtimeSpecification {

    /**
     * Base filter specification from ShowtimeFilterRequest.
     */
    public static Specification<Showtime> withFilters(final ShowtimeFilterRequest filters) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            // Movie filter
            if (filters.getMovieId() != null) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("movie").get("id"), filters.getMovieId()));
            }

            // Screen number filter
            if (filters.getScreenNumber() != null) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("screenNumber"), filters.getScreenNumber()));
            }

            // Start time range
            if (filters.getStartTimeFrom() != null) {
                predicates = cb.and(predicates,
                        cb.greaterThanOrEqualTo(root.get("startTime"), filters.getStartTimeFrom()));
            }

            if (filters.getStartTimeTo() != null) {
                predicates = cb.and(predicates,
                        cb.lessThanOrEqualTo(root.get("startTime"), filters.getStartTimeTo()));
            }

            // Status filters
            if (filters.getStatuses() != null && !filters.getStatuses().isEmpty()) {
                predicates = cb.and(predicates, root.get("status").in(filters.getStatuses()));
            }

            // Optimize queries by fetching movie eagerly to avoid N+1
            if (query != null) {
                query.distinct(true);
                root.fetch("movie", JoinType.LEFT);
            }

            return predicates;
        };
    }

    /**
     * Soft-delete filter.
     */
    public static Specification<Showtime> isNotDeleted() {
        return (root, _, cb) -> cb.isNull(root.get("deletedAt"));
    }

    /**
     * Filter by status list.
     */
    public static Specification<Showtime> hasStatusIn(final List<ShowtimeStatus> statuses) {
        return (root, _, _) -> root.get("status").in(statuses);
    }

    /**
     * Single status filter.
     */
    public static Specification<Showtime> hasStatus(final ShowtimeStatus status) {
        return (root, _, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Future showtimes only (start time > now).
     */
    public static Specification<Showtime> isFuture() {
        return (root, _, cb) -> cb.greaterThan(root.get("startTime"), Instant.now());
    }

    /**
     * Past showtimes only (start time <= now).
     */
    public static Specification<Showtime> isPast() {
        return (root, _, cb) -> cb.lessThanOrEqualTo(root.get("startTime"), Instant.now());
    }

    /**
     * Convenience methods for specific statuses.
     */
    public static Specification<Showtime> isScheduled() {
        return hasStatus(ShowtimeStatus.SCHEDULED);
    }

    public static Specification<Showtime> isCancelled() {
        return hasStatus(ShowtimeStatus.CANCELLED);
    }

    public static Specification<Showtime> isCompleted() {
        return hasStatus(ShowtimeStatus.COMPLETED);
    }

    /**
     * Always-false specification (returns empty results).
     */
    private static Specification<Showtime> alwaysFalse() {
        return (_, _, cb) -> cb.disjunction();
    }

    /**
     * ADMIN VIEW: Compose filter spec for admin (all non-deleted showtimes).
     * Admins can see past, future, cancelled, etc.
     */
    public static Specification<Showtime> forAdmin(final ShowtimeFilterRequest filters) {
        return isNotDeleted().and(withFilters(filters));
    }

    /**
     * CUSTOMER VIEW: Compose filter spec for customers.
     * Only future SCHEDULED showtimes are visible to customers.
     */
    public static Specification<Showtime> forCustomer(final ShowtimeFilterRequest filters) {
        Specification<Showtime> spec = isNotDeleted()
                .and(isFuture())
                .and(isScheduled());

        // Handle status filtering for customers
        if (filters.getStatuses() != null && !filters.getStatuses().isEmpty()) {
            // Customers can only see SCHEDULED showtimes
            if (filters.getStatuses().contains(ShowtimeStatus.SCHEDULED)) {
                // Already applied above, no additional filter needed
            } else {
                // Customer trying to filter for non-SCHEDULED statuses - return empty
                return spec.and(alwaysFalse());
            }
        }

        // Apply other filters (exclude statuses using helper)
        spec = spec.and(withFilters(filters.withoutStatuses()));

        return spec;
    }

    /**
     * Check if showtime overlaps with given time range in the same screen.
     * Used for preventing double-booking of screens.
     */
    public static Specification<Showtime> overlapsInScreen(
            final Short screenNumber,
            final Instant startTime,
            final Instant endTime) {
        return (root, _, cb) -> cb.and(
                cb.equal(root.get("screenNumber"), screenNumber),
                cb.equal(root.get("status"), ShowtimeStatus.SCHEDULED),
                cb.or(
                        // New showtime starts during existing showtime
                        cb.and(
                                cb.lessThanOrEqualTo(root.get("startTime"), startTime),
                                cb.greaterThan(root.get("endTime"), startTime)),
                        // New showtime ends during existing showtime
                        cb.and(
                                cb.lessThan(root.get("startTime"), endTime),
                                cb.greaterThanOrEqualTo(root.get("endTime"), endTime)),
                        // New showtime completely contains existing showtime
                        cb.and(
                                cb.greaterThanOrEqualTo(root.get("startTime"), startTime),
                                cb.lessThanOrEqualTo(root.get("endTime"), endTime))));
    }
}