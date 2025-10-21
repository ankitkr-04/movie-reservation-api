package com.moviereservation.api.repository.specification;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.moviereservation.api.domain.entities.Movie;
import com.moviereservation.api.domain.enums.MovieStatus;
import com.moviereservation.api.web.dto.request.movie.FilterMovieRequest;

public class MovieSpecification {

    /**
     * Base filter specification from FilterMovieRequest.
     * Does NOT include role-based status filtering - that's handled separately.
     */
    public static Specification<Movie> withFilters(final FilterMovieRequest filters) {
        return (root, _, cb) -> {
            var predicates = cb.conjunction();

            if (filters.getTitle() != null && !filters.getTitle().isBlank()) {
                predicates = cb.and(predicates,
                        cb.like(
                                cb.lower(root.get("title")),
                                "%" + filters.getTitle().toLowerCase() + "%"));
            }

            // Only apply status filter if explicitly provided AND not empty
            // (Customer endpoints will override this anyway)
            if (filters.getStatuses() != null && !filters.getStatuses().isEmpty()) {
                predicates = cb.and(predicates,
                        root.get("status").in(filters.getStatuses()));
            }

            if (filters.getGenres() != null && !filters.getGenres().isEmpty()) {
                predicates = cb.and(predicates,
                        root.get("genre").in(filters.getGenres()));
            }

            if (filters.getReleaseDateFrom() != null) {
                predicates = cb.and(predicates,
                        cb.greaterThanOrEqualTo(
                                root.get("releaseDate"), filters.getReleaseDateFrom()));
            }

            if (filters.getReleaseDateTo() != null) {
                predicates = cb.and(predicates,
                        cb.lessThanOrEqualTo(
                                root.get("releaseDate"), filters.getReleaseDateTo()));
            }

            if (filters.getDurationMin() != null) {
                predicates = cb.and(predicates,
                        cb.greaterThanOrEqualTo(
                                root.get("duration"), filters.getDurationMin()));
            }

            if (filters.getDurationMax() != null) {
                predicates = cb.and(predicates,
                        cb.lessThanOrEqualTo(
                                root.get("duration"), filters.getDurationMax()));
            }

            return predicates;
        };
    }

    /**
     * Soft-delete filter (applied everywhere via @SQLRestriction, but explicit for
     * clarity).
     */
    public static Specification<Movie> isNotDeleted() {
        return (root, _, cb) -> cb.isNull(root.get("deletedAt"));
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
        return (root, _, _) -> root.get("status").in(List.of(MovieStatus.ACTIVE, MovieStatus.COMING_SOON));
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
     * Admins can filter by any status via FilterMovieRequest.statuses.
     */
    public static Specification<Movie> forAdmin(final FilterMovieRequest filters) {
        return Specification.<Movie>unrestricted().and(isNotDeleted())
                .and(withFilters(filters));
    }

    public static Specification<Movie> forCustomer(final FilterMovieRequest filters) {
        Specification<Movie> spec = Specification.<Movie>unrestricted().and(isNotDeleted());

        if (filters.getStatuses() != null && !filters.getStatuses().isEmpty()) {
            final List<MovieStatus> allowedStatuses = filters.getStatuses().stream()
                    .filter(status -> status == MovieStatus.ACTIVE || status == MovieStatus.COMING_SOON)
                    .toList();

            // FIXED: Return empty if customer tries to filter only INACTIVE
            if (allowedStatuses.isEmpty()) {
                spec = spec.and((_, _, cb) -> cb.disjunction()); // Always false
            } else {
                spec = spec.and((root, _, _) -> root.get("status").in(allowedStatuses));
            }
        } else {
            spec = spec.and(isVisibleToCustomers());
        }

        // Apply other filters (exclude status from withFilters call)
        spec = spec.and(withFilters(FilterMovieRequest.builder()
                .title(filters.getTitle())
                .genres(filters.getGenres())
                .statuses(null)
                .releaseDateFrom(filters.getReleaseDateFrom())
                .releaseDateTo(filters.getReleaseDateTo())
                .durationMin(filters.getDurationMin())
                .durationMax(filters.getDurationMax())
                .build()));

        return spec;
    }
}