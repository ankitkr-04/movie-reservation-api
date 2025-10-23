package com.moviereservation.api.repository.specification;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.moviereservation.api.domain.entities.Reservation;
import com.moviereservation.api.domain.enums.ReservationStatus;
import com.moviereservation.api.web.dto.request.reservation.ReservationFilterRequest;

import jakarta.persistence.criteria.JoinType;

/**
 * JPA Specifications for Reservation entity filtering.
 * Handles both admin (all reservations) and customer (own reservations)
 * contexts.
 */
public class ReservationSpecification {

    /**
     * Base filter specification from ReservationFilterRequest.
     * Does NOT include userId filtering - that's handled separately for security.
     */
    public static Specification<Reservation> withFilters(final ReservationFilterRequest filters) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            // Status filters
            if (filters.getStatuses() != null && !filters.getStatuses().isEmpty()) {
                predicates = cb.and(predicates, root.get("status").in(filters.getStatuses()));
            }

            // Movie/Showtime filters
            if (filters.getMovieId() != null) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("showtime").get("movie").get("id"), filters.getMovieId()));
            }

            if (filters.getMovieTitle() != null && !filters.getMovieTitle().isBlank()) {
                predicates = cb.and(predicates,
                        cb.like(
                                cb.lower(root.get("showtime").get("movie").get("title")),
                                "%" + filters.getMovieTitle().toLowerCase() + "%"));
            }

            if (filters.getShowtimeId() != null) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("showtime").get("id"), filters.getShowtimeId()));
            }

            if (filters.getScreenNumber() != null) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("showtime").get("screenNumber"), filters.getScreenNumber()));
            }

            // Price range
            if (filters.getMinPrice() != null) {
                predicates = cb.and(predicates,
                        cb.greaterThanOrEqualTo(root.get("totalPrice"), filters.getMinPrice()));
            }

            if (filters.getMaxPrice() != null) {
                predicates = cb.and(predicates,
                        cb.lessThanOrEqualTo(root.get("totalPrice"), filters.getMaxPrice()));
            }

            // Reservation creation date range
            if (filters.getCreatedFrom() != null) {
                predicates = cb.and(predicates,
                        cb.greaterThanOrEqualTo(root.get("createdAt"), filters.getCreatedFrom()));
            }

            if (filters.getCreatedTo() != null) {
                predicates = cb.and(predicates,
                        cb.lessThanOrEqualTo(root.get("createdAt"), filters.getCreatedTo()));
            }

            // Showtime date range
            if (filters.getShowtimeFrom() != null) {
                predicates = cb.and(predicates,
                        cb.greaterThanOrEqualTo(
                                root.get("showtime").get("startTime"), filters.getShowtimeFrom()));
            }

            if (filters.getShowtimeTo() != null) {
                predicates = cb.and(predicates,
                        cb.lessThanOrEqualTo(
                                root.get("showtime").get("startTime"), filters.getShowtimeTo()));
            }

            // Enable fetching to avoid N+1 queries
            if (query != null) {
                query.distinct(true);
                root.fetch("user", JoinType.LEFT);
                root.fetch("showtime", JoinType.LEFT)
                        .fetch("movie", JoinType.LEFT);
                root.fetch("reservationSeats", JoinType.LEFT)
                        .fetch("seatInstance", JoinType.LEFT);
            }

            return predicates;
        };
    }

    /**
     * User-specific filters (admin only).
     * Applied separately for security reasons.
     */
    public static Specification<Reservation> withUserFilters(final ReservationFilterRequest filters) {
        return (root, _, cb) -> {
            var predicates = cb.conjunction();

            // Exact user ID match
            if (filters.getUserId() != null) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("user").get("id"), filters.getUserId()));
            }

            // User email search (partial match)
            if (filters.getUserEmail() != null && !filters.getUserEmail().isBlank()) {
                predicates = cb.and(predicates,
                        cb.like(
                                cb.lower(root.get("user").get("email")),
                                "%" + filters.getUserEmail().toLowerCase() + "%"));
            }

            // User name search (partial match)
            if (filters.getUserName() != null && !filters.getUserName().isBlank()) {
                predicates = cb.and(predicates,
                        cb.like(
                                cb.lower(root.get("user").get("fullName")),
                                "%" + filters.getUserName().toLowerCase() + "%"));
            }

            return predicates;
        };
    }

    /**
     * Filter by specific user ID.
     * Critical for customer context - ensures users only see their own
     * reservations.
     */
    public static Specification<Reservation> belongsToUser(final UUID userId) {
        return (root, _, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    /**
     * Filter by specific status.
     */
    public static Specification<Reservation> hasStatus(final ReservationStatus status) {
        return (root, _, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Filter by booking reference.
     */
    public static Specification<Reservation> hasBookingReference(final String bookingReference) {
        return (root, _, cb) -> cb.equal(
                cb.upper(root.get("bookingReference")),
                bookingReference.toUpperCase());
    }

    /**
     * ADMIN VIEW: Compose filter spec for admin.
     * Includes user filters and all reservations.
     */
    public static Specification<Reservation> forAdmin(final ReservationFilterRequest filters) {
        Specification<Reservation> spec = Specification.allOf();

        // Apply base filters
        spec = spec.and(withFilters(filters));

        // Apply user filters if present
        if (filters.hasUserFilters()) {
            spec = spec.and(withUserFilters(filters));
        }

        return spec;
    }

    /**
     * CUSTOMER VIEW: Compose filter spec for customer.
     * Automatically restricts to user's own reservations.
     */
    public static Specification<Reservation> forCustomer(final UUID customerId,
            final ReservationFilterRequest filters) {
        // Start with user restriction
        Specification<Reservation> spec = belongsToUser(customerId);

        // Apply other filters (sanitized - user filters ignored)
        spec = spec.and(withFilters(filters.withoutUserFilters()));

        return spec;
    }

    /**
     * Get reservation by booking reference for a specific user.
     * Used when customers look up their own reservations.
     */
    public static Specification<Reservation> byBookingReferenceAndUser(
            final String bookingReference, final UUID userId) {
        return hasBookingReference(bookingReference).and(belongsToUser(userId));
    }

    /**
     * Get reservation by booking reference (admin only).
     */
    public static Specification<Reservation> byBookingReference(final String bookingReference) {
        return hasBookingReference(bookingReference);
    }
}