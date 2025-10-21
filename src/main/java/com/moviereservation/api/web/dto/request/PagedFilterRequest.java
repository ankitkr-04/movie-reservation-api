package com.moviereservation.api.web.dto.request;

import java.util.function.Supplier;

import org.springframework.data.domain.Pageable;

import com.moviereservation.api.constant.PaginationDefaults;
import com.moviereservation.api.util.PaginationUtil;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic wrapper combining pagination + filters.
 * 
 * Usage:
 * 
 * <pre>
 * &#64;GetMapping
 * public ResponseEntity<?> getMovies(
 *         @ModelAttribute PagedFilterRequest<MovieFilterDto> request) {
 * 
 *     Pageable pageable = request.toPageable();
 *     MovieFilterDto filters = request.getFilters();
 * 
 *     Page<Movie> movies = service.getAll(pageable, filters);
 * }
 * </pre>
 * 
 * @param <F> Filter DTO type (MovieFilterDto, ShowtimeFilterDto, etc.)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedFilterRequest<F> {

    // ========== Pagination Fields ==========

    @Parameter(description = "Page number (0-indexed)", example = "0")
    private Integer page;

    @Parameter(description = "Page size (max " + PaginationDefaults.MAX_PAGE_SIZE + ")", example = ""
            + PaginationDefaults.DEFAULT_PAGE_SIZE)
    private Integer size;

    @Parameter(description = "Sort field name", example = "createdAt")
    private String sortBy;

    @Parameter(description = "Sort direction (ASC or DESC)", example = "DESC")
    private String sortDirection;

    // ========== Filter Object ==========

    /**
     * Entity-specific filters (MovieFilterDto, ShowtimeFilterDto, etc.)
     * This will be flattened as query params by Spring.
     */
    private F filters;

    // ========== Helper Methods ==========

    /**
     * Convert to Spring Pageable with validation.
     */
    public Pageable toPageable() {
        return PaginationUtil.createPageable(
                page, size, sortBy, sortDirection);
    }

    /**
     * Get filters or empty instance if null.
     * Prevents NullPointerException in service layer.
     */
    public F getFiltersOrEmpty(final Supplier<F> emptySupplier) {
        return filters != null ? filters : emptySupplier.get();
    }
}