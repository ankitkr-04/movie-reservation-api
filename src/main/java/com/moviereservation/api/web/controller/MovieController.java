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
 */
@RestController
@RequestMapping(Route.MOVIES)
@Tag(name = "Movies", description = "Public movie browsing for customers")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final MovieMapper movieMapper;

    @GetMapping
    @Operation(summary = "Browse all movies (Customer)")
    public ResponseEntity<ApiResponse<PagedResponse<MovieCustomerResponse>>> getAllMovies(
            @ModelAttribute @Valid PagedFilterRequest<MovieFilterRequest> request) {

        final Pageable pageable = request.toPageable();
        final MovieFilterRequest filters = request.getFiltersOrEmpty(MovieFilterRequest::new);

        final Page<Movie> movies = movieService.findAllForCustomer(pageable, filters);
        final PagedResponse<MovieCustomerResponse> response = PagedResponse.of(movies, movieMapper::toCustomerResponse);

        return ResponseEntity.ok(ApiResponse.success("Movies fetched successfully", response));
    }

    @GetMapping("/{movieId}")
    @Operation(summary = "Get movie details (Customer)")
    public ResponseEntity<ApiResponse<MovieCustomerResponse>> getMovieById(
            @PathVariable final UUID movieId) {
        final Movie movie = movieService.findByIdForCustomer(movieId);
        return ResponseEntity.ok(ApiResponse.success(
                "Movie fetched successfully", movieMapper.toCustomerResponse(movie)));
    }

    @GetMapping("/active")
    @Operation(summary = "Get only active movies")
    public ResponseEntity<ApiResponse<PagedResponse<MovieCustomerResponse>>> getActiveMovies(
            @ModelAttribute @Valid PagedFilterRequest<MovieFilterRequest> request) {

        final Pageable pageable = request.toPageable();
        final MovieFilterRequest filters = request.getFiltersOrEmpty(MovieFilterRequest::new);

        // Create new filter instance with ACTIVE status (immutable approach)
        final MovieFilterRequest activeFilter = filters.toBuilder()
                .statuses(List.of(MovieStatus.ACTIVE))
                .build();

        final Page<Movie> movies = movieService.findAllForCustomer(pageable, activeFilter);
        final PagedResponse<MovieCustomerResponse> response = PagedResponse.of(movies, movieMapper::toCustomerResponse);

        return ResponseEntity.ok(ApiResponse.success("Active movies fetched successfully", response));
    }

    @GetMapping("/coming-soon")
    @Operation(summary = "Get upcoming movies")
    public ResponseEntity<ApiResponse<PagedResponse<MovieCustomerResponse>>> getComingSoonMovies(
            @ModelAttribute @Valid PagedFilterRequest<MovieFilterRequest> request) {

        final Pageable pageable = request.toPageable();
        final MovieFilterRequest filters = request.getFiltersOrEmpty(MovieFilterRequest::new);

        // Create new filter instance with COMING_SOON status (immutable approach)
        final MovieFilterRequest comingSoonFilter = filters.toBuilder()
                .statuses(List.of(MovieStatus.COMING_SOON))
                .build();

        final Page<Movie> movies = movieService.findAllForCustomer(pageable, comingSoonFilter);
        final PagedResponse<MovieCustomerResponse> response = PagedResponse.of(movies, movieMapper::toCustomerResponse);

        return ResponseEntity.ok(ApiResponse.success("Coming soon movies fetched successfully", response));
    }
}
