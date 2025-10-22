package com.moviereservation.api.repository.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.moviereservation.api.domain.entities.SeatInstance;
import com.moviereservation.api.web.dto.request.seat.SeatMapFilterRequest;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class SeatSpecification {

    public static Specification<SeatInstance> withFilters(final SeatMapFilterRequest filters, final UUID showtimeId) {
        return (final Root<SeatInstance> root, final CriteriaQuery<?> _, final CriteriaBuilder cb) -> {
            final List<Predicate> predicates = new ArrayList<>();

            // Always filter by showtimeId
            predicates.add(cb.equal(root.get("showtime").get("id"), showtimeId));

            // Filter by rowLabels if provided
            if (!filters.getRowLabelsAsList().isEmpty()) {
                predicates.add(root.get("rowLabel").in(filters.getRowLabelsAsList()));
            }

            // Ensure only non-deleted seats are returned (SQLRestriction: deleted_at IS
            // NULL)
            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}