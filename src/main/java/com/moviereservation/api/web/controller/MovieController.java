package com.moviereservation.api.web.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.moviereservation.api.constant.Route;
import com.moviereservation.api.domain.entities.Movie;
import com.moviereservation.api.domain.enums.MovieStatus;
import com.moviereservation.api.service.MovieService;
import com.moviereservation.api.web.dto.request.PagedFilterRequest;
import com.moviereservation.api.web.dto.request.movie.MovieFilterRequest;
import com.moviereservation.api.web.dto.response.movie.MovieCustomerResponse;
import com.moviereservation.api.web.dto.response.wrappers.ApiResponse;
import com.moviereservation.api.web.dto.response.wrappers.PagedResponse;
import com.moviereservation.api.web.mapper.MovieMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Public movie browsing endpoints for customers.
 * Only shows ACTIVE and COMING_SOON movies.
 */
@RestController
@RequestMapping(Route.MOVIES)
@Tag(name = "Movies (Customer)", description = "Browse movies - ACTIVE and COMING_SOON only")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final MovieMapper movieMapper;

    /**
     * Browse all customer-visible movies (ACTIVE + COMING_SOON).
     * Supports filtering by genre, title, release date, duration.
     */
    @GetMapping
    @Operation(summary = "Browse movies", description = "Get paginated list of ACTIVE and COMING_SOON movies with optional filters")
    public ResponseEntity<ApiResponse<PagedResponse<MovieCustomerResponse>>> browseMovies(
            @ModelAttribute @Valid PagedFilterRequest<MovieFilterRequest> request) {

        Pageable pageable = request.toPageable();
        MovieFilterRequest filters = request.getFiltersOrEmpty(MovieFilterRequest::new);

        Page<Movie> movies = movieService.findAllForCustomer(pageable, filters);
        PagedResponse<MovieCustomerResponse> response = PagedResponse.of(movies, movieMapper::toCustomerResponse);

        return ResponseEntity.ok(
                ApiResponse.success("Movies retrieved successfully",
                        response));
    }

    /**
     * Get single movie details by ID.
     * Only returns movie if it's ACTIVE or COMING_SOON.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get movie by ID", description = "Get detailed movie information (customer-visible movies only)")
    public ResponseEntity<ApiResponse<MovieCustomerResponse>> getMovie(
            @PathVariable("id") UUID movieId) {

        Movie movie = movieService.findByIdForCustomer(movieId);

        return ResponseEntity.ok(
                ApiResponse.success("Movie retrieved successfully",
                        movieMapper.toCustomerResponse(movie)));
    }

    /**
     * Get only ACTIVE movies (currently showing).
     * Convenience endpoint for "Now Showing" section.
     */
    @GetMapping("/now-showing")
    @Operation(summary = "Get now showing movies", description = "Get only ACTIVE movies currently in theaters")
    public ResponseEntity<ApiResponse<PagedResponse<MovieCustomerResponse>>> getNowShowing(
            @ModelAttribute @Valid PagedFilterRequest<MovieFilterRequest> request) {

        Pageable pageable = request.toPageable();
        MovieFilterRequest filters = request.getFiltersOrEmpty(MovieFilterRequest::new);

        // Override status to ACTIVE only
        MovieFilterRequest activeFilter = filters.toBuilder()
                .statuses(List.of(MovieStatus.ACTIVE))
                .build();

        Page<Movie> movies = movieService.findAllForCustomer(pageable, activeFilter);
        PagedResponse<MovieCustomerResponse> response = PagedResponse.of(movies, movieMapper::toCustomerResponse);
        return ResponseEntity.ok(
                ApiResponse.success("Now showing movies retrieved successfully", response));
    }

    /**
     * Get only COMING_SOON movies.
     * Convenience endpoint for "Coming Soon" section.
     */
    @GetMapping("/coming-soon")
    @Operation(summary = "Get coming soon movies", description = "Get upcoming movies not yet released")
    public ResponseEntity<ApiResponse<PagedResponse<MovieCustomerResponse>>> getComingSoon(
            @ModelAttribute @Valid PagedFilterRequest<MovieFilterRequest> request) {

        Pageable pageable = request.toPageable();
        MovieFilterRequest filters = request.getFiltersOrEmpty(MovieFilterRequest::new);

        // Override status to COMING_SOON only
        MovieFilterRequest comingSoonFilter = filters.toBuilder()
                .statuses(List.of(MovieStatus.COMING_SOON))
                .build();

        Page<Movie> movies = movieService.findAllForCustomer(pageable, comingSoonFilter);

        PagedResponse<MovieCustomerResponse> response = PagedResponse.of(movies, movieMapper::toCustomerResponse);
        return ResponseEntity.ok(
                ApiResponse.success("Coming soon movies retrieved successfully", response));
    }
}