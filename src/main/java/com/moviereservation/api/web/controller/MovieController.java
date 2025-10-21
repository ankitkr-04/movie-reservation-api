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
import com.moviereservation.api.web.dto.request.movie.FilterMovieRequest;
import com.moviereservation.api.web.dto.response.movie.MovieResponse;
import com.moviereservation.api.web.dto.response.wrappers.ApiResponse;
import com.moviereservation.api.web.dto.response.wrappers.PagedResponse;
import com.moviereservation.api.web.mapper.MovieMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(Route.MOVIES)
@Tag(name = "Movies", description = "Public movie browsing for customers")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final MovieMapper movieMapper;

    @GetMapping
    @Operation(summary = "Browse movies (Customer)")
    public ResponseEntity<ApiResponse<PagedResponse<MovieResponse>>> getAllMovies(
            @ModelAttribute @Valid PagedFilterRequest<FilterMovieRequest> request) {

        final Pageable pageable = request.toPageable();
        final FilterMovieRequest filters = request.getFiltersOrEmpty(FilterMovieRequest::new);

        final Page<Movie> movies = movieService.getAllMoviesForCustomer(pageable, filters);
        final PagedResponse<MovieResponse> response = PagedResponse.of(movies, movieMapper::toCustomerResponse);

        // WRAP in ApiResponse for consistency
        return ResponseEntity.ok(ApiResponse.success("Movies fetched successfully", response));
    }

    @GetMapping("/{movieId}")
    @Operation(summary = "Get movie details (Customer)")
    public ResponseEntity<ApiResponse<MovieResponse>> getMovieById(@PathVariable final UUID movieId) {
        final Movie movie = movieService.getMovieByIdForCustomer(movieId); // CHANGED
        return ResponseEntity.ok(ApiResponse.success(
                "Movie fetched successfully", movieMapper.toCustomerResponse(movie)));
    }

    @GetMapping("/active")
    @Operation(summary = "Get only active movies")
    public ResponseEntity<ApiResponse<PagedResponse<MovieResponse>>> getActiveMovies(
            @ModelAttribute @Valid PagedFilterRequest<FilterMovieRequest> request) {

        final Pageable pageable = request.toPageable();
        final FilterMovieRequest filters = request.getFiltersOrEmpty(FilterMovieRequest::new);

        // FORCE ACTIVE status only
        filters.setStatuses(List.of(MovieStatus.ACTIVE));

        final Page<Movie> movies = movieService.getAllMoviesForCustomer(pageable, filters);
        final PagedResponse<MovieResponse> response = PagedResponse.of(movies, movieMapper::toCustomerResponse);

        return ResponseEntity.ok(ApiResponse.success("Active movies fetched successfully", response));
    }

    @GetMapping("/coming-soon")
    @Operation(summary = "Get upcoming movies")
    public ResponseEntity<ApiResponse<PagedResponse<MovieResponse>>> getComingSoonMovies(
            @ModelAttribute @Valid PagedFilterRequest<FilterMovieRequest> request) {

        final Pageable pageable = request.toPageable();
        final FilterMovieRequest filters = request.getFiltersOrEmpty(FilterMovieRequest::new);

        // FORCE COMING_SOON status only
        filters.setStatuses(List.of(MovieStatus.COMING_SOON));

        final Page<Movie> movies = movieService.getAllMoviesForCustomer(pageable, filters);
        final PagedResponse<MovieResponse> response = PagedResponse.of(movies, movieMapper::toCustomerResponse);

        return ResponseEntity.ok(ApiResponse.success("Coming soon movies fetched successfully", response));
    }
}
