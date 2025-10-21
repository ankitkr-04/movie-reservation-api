package com.moviereservation.api.web.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.moviereservation.api.constant.Route;
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

@RestController
@RequestMapping(Route.SHOWTIMES)
@Tag(name = "Showtimes", description = "Public showtime browsing for customers")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeService showtimeService;
    private final ShowtimeMapper showtimeMapper;

    @GetMapping
    @Operation(summary = "Browse showtimes (Customer)", description = "View upcoming showtimes with optional filtering")
    public ResponseEntity<ApiResponse<PagedResponse<ShowtimeCustomerResponse>>> getShowtimes(
            @ModelAttribute @Valid final PagedFilterRequest<ShowtimeFilterRequest> request) {

        final Pageable pageable = request.toPageable();
        final ShowtimeFilterRequest filters = request.getFiltersOrEmpty(ShowtimeFilterRequest::new);

        final var pagedShowtimes = showtimeService.findAllForCustomer(pageable, filters);
        final var pagedResponses = PagedResponse.of(pagedShowtimes, showtimeMapper::toCustomerResponse);

        return ResponseEntity.ok(ApiResponse.success("Showtimes fetched successfully", pagedResponses));
    }

    @GetMapping("/movie/{movieId}")
    @Operation(summary = "Get showtimes for a movie")
    public ResponseEntity<ApiResponse<PagedResponse<ShowtimeCustomerResponse>>> getShowtimesByMovie(
            @PathVariable final UUID movieId,
            @ModelAttribute @Valid final PagedFilterRequest<ShowtimeFilterRequest> request) {

        final Pageable pageable = request.toPageable();
        final ShowtimeFilterRequest filters = request.getFiltersOrEmpty(ShowtimeFilterRequest::new);

        // Override movieId filter
        final ShowtimeFilterRequest movieFilter = filters.toBuilder()
                .movieId(movieId)
                .build();

        final var pagedShowtimes = showtimeService.findAllForCustomer(pageable, movieFilter);
        final var pagedResponses = PagedResponse.of(pagedShowtimes, showtimeMapper::toCustomerResponse);

        return ResponseEntity.ok(ApiResponse.success("Showtimes fetched successfully", pagedResponses));
    }
}