package com.moviereservation.api.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.moviereservation.api.constant.PaginationDefaults;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class PaginationUtil {

    /**
     * Create Pageable with validation and defaults.
     * 
     * @param page          Page number (nullable)
     * @param size          Page size (nullable)
     * @param sortBy        Sort field (nullable)
     * @param sortDirection Sort direction (nullable, "ASC" or "DESC")
     * @return Validated Pageable object
     */
    public static Pageable createPageable(
            final Integer page,
            final Integer size,
            final String sortBy,
            final String sortDirection) {

        // Validate and apply defaults
        final int pageNumber = validatePage(page);
        final int pageSize = validateSize(size);
        final Sort sort = createSort(sortBy, sortDirection);

        return PageRequest.of(pageNumber, pageSize, sort);
    }

    /**
     * Validate page number (must be >= 0).
     */
    private static int validatePage(final Integer page) {
        if (page == null || page < 0) {
            return PaginationDefaults.DEFAULT_PAGE_NUMBER;
        }
        return page;
    }

    /**
     * Validate page size (must be > 0 and <= MAX).
     */
    private static int validateSize(final Integer size) {
        if (size == null || size <= 0) {
            return PaginationDefaults.DEFAULT_PAGE_SIZE;
        }
        // Cap at maximum to prevent abuse
        return Math.min(size, PaginationDefaults.MAX_PAGE_SIZE);
    }

    /**
     * Create Sort object with validation.
     */
    private static Sort createSort(final String sortBy, final String sortDirection) {
        final String field = (sortBy != null && !sortBy.isBlank())
                ? sortBy
                : PaginationDefaults.DEFAULT_SORT_FIELD;

        final Sort.Direction direction = parseSortDirection(sortDirection);

        return Sort.by(direction, field);
    }

    /**
     * Parse sort direction string to Sort.Direction enum.
     */
    private static Sort.Direction parseSortDirection(final String sortDirection) {
        if (sortDirection == null || sortDirection.isBlank()) {
            return Sort.Direction.fromString(PaginationDefaults.DEFAULT_SORT_DIRECTION);
        }

        try {
            return Sort.Direction.fromString(sortDirection.toUpperCase());
        } catch (final IllegalArgumentException e) {
            // Invalid direction, use default
            return Sort.Direction.fromString(PaginationDefaults.DEFAULT_SORT_DIRECTION);
        }
    }
}