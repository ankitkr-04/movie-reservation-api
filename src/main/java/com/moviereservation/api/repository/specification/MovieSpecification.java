package com.moviereservation.api.repository.specification;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.moviereservation.api.domain.entities.Movie;
import com.moviereservation.api.domain.enums.MovieStatus;
import com.moviereservation.api.web.dto.request.movie.MovieFilterRequest;

/**
 * JPA Specifications for Movie entity filtering.
 * Provides composable query predicates for different contexts (admin,
 * customer).
 */
public class MovieSpecification {

    // Customer-visible statuses
    private static final List<MovieStatus> CUSTOMER_VISIBLE_STATUSES = List.of(
            MovieStatus.ACTIVE,
            MovieStatus.COMING_SOON);

    /**
     * Base filter specification from MovieFilterRequest.
     * Does NOT include role-based status filtering - that's handled separately.
     */
    public static Specification<Movie> withFilters(final MovieFilterRequest filters) {
        return (root, _, cb) -> {
            var predicates = cb.conjunction();

            // Title search (case-insensitive partial match)
            if (filters.getTitle() != null && !filters.getTitle().isBlank()) {
                predicates = cb.and(predicates,
                        cb.like(
                                cb.lower(root.get("title")),
                                "%" + filters.getTitle().toLowerCase() + "%"));
            }

            // Status filters (only if explicitly provided)
            if (filters.getStatuses() != null && !filters.getStatuses().isEmpty()) {
                predicates = cb.and(predicates, root.get("status").in(filters.getStatuses()));
            }

            // Genre filters
            if (filters.getGenres() != null && !filters.getGenres().isEmpty()) {
                predicates = cb.and(predicates, root.get("genre").in(filters.getGenres()));
            }

            // Release date range
            if (filters.getReleaseDateFrom() != null) {
                predicates = cb.and(predicates,
                        cb.greaterThanOrEqualTo(root.get("releaseDate"), filters.getReleaseDateFrom()));
            }

            if (filters.getReleaseDateTo() != null) {
                predicates = cb.and(predicates,
                        cb.lessThanOrEqualTo(root.get("releaseDate"), filters.getReleaseDateTo()));
            }

            // Duration range
            if (filters.getDurationMin() != null) {
                predicates = cb.and(predicates,
                        cb.greaterThanOrEqualTo(root.get("duration"), filters.getDurationMin()));
            }

            if (filters.getDurationMax() != null) {
                predicates = cb.and(predicates,
                        cb.lessThanOrEqualTo(root.get("duration"), filters.getDurationMax()));
            }

            return predicates;
        };
    }

    /**
     * Soft-delete filter.
     * Applied via @SQLRestriction on entity, but explicit for clarity.
     */
    public static Specification<Movie> isNotDeleted() {
        return (root, _, cb) -> cb.isNull(root.get("deletedAt"));
    }

    /**
     * Filter movies by a list of statuses.
     */
    public static Specification<Movie> hasStatusIn(final List<MovieStatus> statuses) {
        return (root, _, _) -> root.get("status").in(statuses);
    }

    /**
     * Single status filter.
     */
    public static Specification<Movie> hasStatus(final MovieStatus status) {
        return (root, _, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * CUSTOMER VISIBLE: Only ACTIVE and COMING_SOON movies.
     * Use this specification for customer-facing endpoints.
     */
    public static Specification<Movie> isVisibleToCustomers() {
        return hasStatusIn(CUSTOMER_VISIBLE_STATUSES);
    }

    /**
     * Always-false specification (returns empty results).
     */
    private static Specification<Movie> alwaysFalse() {
        return (_, _, cb) -> cb.disjunction();
    }

    /**
     * Convenience methods for specific statuses.
     */
    public static Specification<Movie> isActive() {
        return hasStatus(MovieStatus.ACTIVE);
    }

    public static Specification<Movie> isComingSoon() {
        return hasStatus(MovieStatus.COMING_SOON);
    }

    public static Specification<Movie> isInactive() {
        return hasStatus(MovieStatus.INACTIVE);
    }

    /**
     * ADMIN VIEW: Compose filter spec for admin (all non-deleted movies).
     * Admins can filter by any status via MovieFilterRequest.statuses.
     */
    public static Specification<Movie> forAdmin(final MovieFilterRequest filters) {
        return isNotDeleted().and(withFilters(filters));
    }

    /**
     * CUSTOMER VIEW: Compose filter spec for customers.
     * Automatically restricts to visible statuses and sanitizes filters.
     */
    public static Specification<Movie> forCustomer(final MovieFilterRequest filters) {
        Specification<Movie> spec = isNotDeleted();

        // Handle status filtering for customers
        if (filters.getStatuses() != null && !filters.getStatuses().isEmpty()) {
            final List<MovieStatus> allowedStatuses = filters.getStatuses().stream()
                    .filter(CUSTOMER_VISIBLE_STATUSES::contains)
                    .toList();

            // If customer tries to filter only by non-visible statuses, return empty
            if (allowedStatuses.isEmpty()) {
                return spec.and(alwaysFalse());
            }

            spec = spec.and(hasStatusIn(allowedStatuses));
        } else {
            // Default to visible statuses if no status filter provided
            spec = spec.and(isVisibleToCustomers());
        }

        // Apply other filters (exclude status from withFilters call)
        spec = spec.and(withFilters(filters.withoutStatuses()));

        return spec;
    }
}