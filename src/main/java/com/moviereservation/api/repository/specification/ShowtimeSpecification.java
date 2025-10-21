package com.moviereservation.api.repository.specification;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.moviereservation.api.domain.entities.Showtime;
import com.moviereservation.api.domain.enums.ShowtimeStatus;
import com.moviereservation.api.web.dto.request.showtime.ShowtimeFilterRequest;

public class ShowtimeSpecification {

    public static Specification<Showtime> withFilters(final ShowtimeFilterRequest filters) {
        return (root, _, cb) -> {
            var predicates = cb.conjunction();

            if (filters.getMovieId() != null) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("movie").get("id"), filters.getMovieId()));
            }

            if (filters.getScreenNumber() != null) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("screenNumber"), filters.getScreenNumber()));
            }

            if (filters.getStartTimeFrom() != null) {
                predicates = cb.and(predicates,
                        cb.greaterThanOrEqualTo(root.get("startTime"), filters.getStartTimeFrom()));
            }

            if (filters.getStartTimeTo() != null) {
                predicates = cb.and(predicates,
                        cb.lessThanOrEqualTo(root.get("startTime"), filters.getStartTimeTo()));
            }

            if (filters.getStatuses() != null && !filters.getStatuses().isEmpty()) {
                predicates = cb.and(predicates,
                        root.get("status").in(filters.getStatuses()));
            }

            return predicates;
        };
    }

    public static Specification<Showtime> isNotDeleted() {
        return (root, _, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Showtime> hasStatusIn(final List<ShowtimeStatus> statuses) {
        return (root, _, _) -> root.get("status").in(statuses);
    }

    public static Specification<Showtime> hasStatus(final ShowtimeStatus status) {
        return (root, _, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Showtime> isFuture() {
        return (root, _, cb) -> cb.greaterThan(root.get("startTime"), Instant.now());
    }

    public static Specification<Showtime> isScheduled() {
        return hasStatus(ShowtimeStatus.SCHEDULED);
    }

    public static Specification<Showtime> forAdmin(final ShowtimeFilterRequest filters) {
        return isNotDeleted().and(withFilters(filters));
    }

    public static Specification<Showtime> forCustomer(final ShowtimeFilterRequest filters) {
        Specification<Showtime> spec = isNotDeleted()
                .and(isFuture())
                .and(isScheduled());

        if (filters.getStatuses() != null && !filters.getStatuses().isEmpty()) {
            // Customer can only see SCHEDULED showtimes
            if (filters.getStatuses().contains(ShowtimeStatus.SCHEDULED)) {
                spec = spec.and(hasStatus(ShowtimeStatus.SCHEDULED));
            } else {
                // If filtering for non-SCHEDULED, return empty
                return spec.and((_, _, cb) -> cb.disjunction());
            }
        }

        spec = spec.and(withFilters(filters.withoutStatuses()));
        return spec;
    }
}