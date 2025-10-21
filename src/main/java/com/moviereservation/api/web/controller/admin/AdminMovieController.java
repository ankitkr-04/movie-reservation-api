package com.moviereservation.api.web.controller.admin;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.moviereservation.api.constant.Route;
import com.moviereservation.api.domain.entities.Movie;
import com.moviereservation.api.service.MovieService;
import com.moviereservation.api.web.dto.request.PagedFilterRequest;
import com.moviereservation.api.web.dto.request.movie.CreateMovieRequest;
import com.moviereservation.api.web.dto.request.movie.MovieFilterRequest;
import com.moviereservation.api.web.dto.request.movie.UpdateMovieRequest;
import com.moviereservation.api.web.dto.response.movie.MovieAdminResponse;
import com.moviereservation.api.web.dto.response.wrappers.ApiResponse;
import com.moviereservation.api.web.dto.response.wrappers.PagedResponse;
import com.moviereservation.api.web.mapper.MovieMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Admin endpoints for movie management.
 * Full CRUD access with no status restrictions.
 */
@RestController
@RequestMapping(Route.ADMIN + "/movies")
@Tag(name = "Admin - Movies", description = "Movie management for administrators")
@RequiredArgsConstructor
public class AdminMovieController {

        private final MovieService movieService;
        private final MovieMapper movieMapper;

        @PostMapping
        @Operation(summary = "Create new movie")
        public ResponseEntity<ApiResponse<MovieAdminResponse>> createMovie(
                        @Valid @RequestBody final CreateMovieRequest request) {

                final Movie movie = movieService.create(request);
                final MovieAdminResponse response = movieMapper.toAdminResponse(movie);

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Movie created successfully", response));
        }

        @PatchMapping("/{movieId}")
        @Operation(summary = "Update existing movie")
        public ResponseEntity<ApiResponse<MovieAdminResponse>> updateMovie(
                        @PathVariable final UUID movieId,
                        @Valid @RequestBody final UpdateMovieRequest request) {

                final Movie movie = movieService.update(movieId, request);
                final MovieAdminResponse response = movieMapper.toAdminResponse(movie);

                return ResponseEntity.ok(ApiResponse.success("Movie updated successfully", response));
        }

        @GetMapping("/{movieId}")
        @Operation(summary = "Get movie by ID")
        public ResponseEntity<ApiResponse<MovieAdminResponse>> getMovie(
                        @PathVariable final UUID movieId) {

                final Movie movie = movieService.findById(movieId);
                final MovieAdminResponse response = movieMapper.toAdminResponse(movie);

                return ResponseEntity.ok(ApiResponse.success("Movie fetched successfully", response));
        }

        @GetMapping
        @Operation(summary = "Get all movies (Admin)", description = "Retrieve a paginated list of movies with optional filtering. Admins can filter by any status.")
        public ResponseEntity<ApiResponse<PagedResponse<MovieAdminResponse>>> getMovies(
                        @ModelAttribute final PagedFilterRequest<MovieFilterRequest> pagedFilterRequest) {

                final Pageable pageable = pagedFilterRequest.toPageable();
                final MovieFilterRequest filters = pagedFilterRequest.getFiltersOrEmpty(MovieFilterRequest::new);

                final var pagedMovies = movieService.findAllForAdmin(pageable, filters);
                final var pagedResponses = PagedResponse.of(pagedMovies, movieMapper::toAdminResponse);

                return ResponseEntity.ok(ApiResponse.success("Movies fetched successfully", pagedResponses));
        }
}
