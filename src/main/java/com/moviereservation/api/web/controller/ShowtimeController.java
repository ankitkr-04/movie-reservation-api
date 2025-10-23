package com.moviereservation.api.web.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.moviereservation.api.constant.Route;
import com.moviereservation.api.domain.entities.Showtime;
import com.moviereservation.api.service.ShowtimeService;
import com.moviereservation.api.web.dto.request.PagedFilterRequest;
import com.moviereservation.api.web.dto.request.showtime.ShowtimeFilterRequest;
import com.moviereservation.api.web.dto.response.showtime.ShowtimeCustomerResponse;
import com.moviereservation.api.web.dto.response.wrappers.ApiResponse;
import com.moviereservation.api.web.dto.response.wrappers.PagedResponse;
import com.moviereservation.api.web.mapper.ShowtimeMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Public showtime browsing endpoints for customers.
 * Only shows future SCHEDULED showtimes.
 */
@RestController
@RequestMapping(Route.SHOWTIMES)
@Tag(name = "Showtimes (Customer)", description = "Browse showtimes - future SCHEDULED only")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeService showtimeService;
    private final ShowtimeMapper showtimeMapper;

    /**
     * Browse all upcoming showtimes.
     * Automatically filtered to future SCHEDULED showtimes only.
     */
    @GetMapping
    @Operation(summary = "Browse showtimes", description = "Get paginated list of upcoming showtimes with optional filters (movie, screen, date range)")
    public ResponseEntity<ApiResponse<PagedResponse<ShowtimeCustomerResponse>>> browseShowtimes(
            @ModelAttribute @Valid PagedFilterRequest<ShowtimeFilterRequest> request) {

        Pageable pageable = request.toPageable();
        ShowtimeFilterRequest filters = request.getFiltersOrEmpty(ShowtimeFilterRequest::new);

        Page<Showtime> showtimes = showtimeService.findAllForCustomer(pageable, filters);

        PagedResponse<ShowtimeCustomerResponse> response = PagedResponse.of(showtimes,
                showtimeMapper::toCustomerResponse);

        return ResponseEntity.ok(
                ApiResponse.success("Showtimes retrieved successfully", response));
    }

    /**
     * Get single showtime by ID.
     * Only returns if it's a future SCHEDULED showtime.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get showtime by ID", description = "Get detailed showtime information including movie details and available seats count")
    public ResponseEntity<ApiResponse<ShowtimeCustomerResponse>> getShowtime(
            @PathVariable("id") UUID showtimeId) {

        Showtime showtime = showtimeService.findByIdForCustomer(showtimeId);

        return ResponseEntity.ok(
                ApiResponse.success("Showtime retrieved successfully",
                        showtimeMapper.toCustomerResponse(showtime)));
    }

    /**
     * Get all showtimes for a specific movie.
     * Convenience endpoint for "Select Showtime" page.
     */
    @GetMapping("/movie/{movieId}")
    @Operation(summary = "Get showtimes for a movie", description = "Get all upcoming showtimes for a specific movie")
    public ResponseEntity<ApiResponse<PagedResponse<ShowtimeCustomerResponse>>> getShowtimesByMovie(
            @PathVariable UUID movieId,
            @ModelAttribute @Valid PagedFilterRequest<ShowtimeFilterRequest> request) {

        Pageable pageable = request.toPageable();
        ShowtimeFilterRequest filters = request.getFiltersOrEmpty(ShowtimeFilterRequest::new);

        // Override movieId filter
        ShowtimeFilterRequest movieFilter = filters.toBuilder()
                .movieId(movieId)
                .build();

        Page<Showtime> showtimes = showtimeService.findAllForCustomer(pageable, movieFilter);

        PagedResponse<ShowtimeCustomerResponse> response = PagedResponse.of(showtimes,
                showtimeMapper::toCustomerResponse);

        return ResponseEntity.ok(
                ApiResponse.success("Showtimes for movie retrieved successfully", response));
    }

    /**
     * Get showtimes for a specific screen.
     * Useful for checking availability across movies in one screen.
     */
    @GetMapping("/screen/{screenNumber}")
    @Operation(summary = "Get showtimes for a screen", description = "Get all upcoming showtimes for a specific screen")
    public ResponseEntity<ApiResponse<PagedResponse<ShowtimeCustomerResponse>>> getShowtimesByScreen(
            @PathVariable Short screenNumber,
            @ModelAttribute @Valid PagedFilterRequest<ShowtimeFilterRequest> request) {

        Pageable pageable = request.toPageable();
        ShowtimeFilterRequest filters = request.getFiltersOrEmpty(ShowtimeFilterRequest::new);

        // Override screenNumber filter
        ShowtimeFilterRequest screenFilter = filters.toBuilder()
                .screenNumber(screenNumber)
                .build();

        Page<Showtime> showtimes = showtimeService.findAllForCustomer(pageable, screenFilter);

        PagedResponse<ShowtimeCustomerResponse> response = PagedResponse.of(showtimes,
                showtimeMapper::toCustomerResponse);
        return ResponseEntity.ok(
                ApiResponse.success("Showtimes for screen retrieved successfully", response));
    }
}