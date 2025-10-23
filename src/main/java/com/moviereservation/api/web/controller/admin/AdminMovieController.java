package com.moviereservation.api.web.controller.admin;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.moviereservation.api.constant.Route;
import com.moviereservation.api.domain.entities.Movie;
import com.moviereservation.api.domain.enums.MovieStatus;
import com.moviereservation.api.security.UserPrincipal;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Admin movie management endpoints.
 * Full CRUD access to all movies regardless of status.
 */
@RestController
@RequestMapping(Route.ADMIN + "/movies")
@Tag(name = "Admin - Movies", description = "Movie management for administrators")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminMovieController {

        private final MovieService movieService;
        private final MovieMapper movieMapper;

        /**
         * Create a new movie.
         * Default status is COMING_SOON if not specified.
         */
        @PostMapping
        @Operation(summary = "Create movie", description = "Create a new movie. Default status: COMING_SOON")
        public ResponseEntity<ApiResponse<MovieAdminResponse>> createMovie(
                        @Valid @RequestBody CreateMovieRequest request) {

                Movie movie = movieService.create(request);

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Movie created successfully",
                                                movieMapper.toAdminResponse(movie)));
        }

        /**
         * Update existing movie.
         * Only non-null fields are updated (partial update).
         */
        @PatchMapping("/{id}")
        @Operation(summary = "Update movie", description = "Partially update movie. Only provided fields will be updated.")
        public ResponseEntity<ApiResponse<MovieAdminResponse>> updateMovie(
                        @PathVariable("id") UUID movieId,
                        @Valid @RequestBody UpdateMovieRequest request) {

                Movie movie = movieService.update(movieId, request);

                return ResponseEntity.ok(
                                ApiResponse.success("Movie updated successfully", movieMapper.toAdminResponse(movie)));
        }

        /**
         * Get movie by ID (admin view with full details).
         * Can retrieve movies in any status including INACTIVE.
         */
        @GetMapping("/{id}")
        @Operation(summary = "Get movie by ID", description = "Get movie details including administrative metadata")
        public ResponseEntity<ApiResponse<MovieAdminResponse>> getMovie(
                        @PathVariable("id") UUID movieId) {

                Movie movie = movieService.findById(movieId);

                return ResponseEntity.ok(
                                ApiResponse.success("Movie retrieved successfully",
                                                movieMapper.toAdminResponse(movie)));
        }

        /**
         * Soft delete a movie.
         * Cannot delete if movie has:
         * - Future showtimes
         * - Active reservations
         */
        @DeleteMapping("/{id}")
        @Operation(summary = "Delete movie", description = "Soft delete movie. Cannot delete if it has future showtimes or active reservations.")
        public ResponseEntity<ApiResponse<Void>> deleteMovie(
                        @PathVariable("id") UUID movieId,
                        @AuthenticationPrincipal UserPrincipal principal) {

                UUID adminId = principal.getUserId();
                movieService.delete(movieId, adminId);

                return ResponseEntity.ok(
                                ApiResponse.success("Movie deleted successfully"));
        }

        /**
         * Browse all movies with filters.
         * Admin can see movies in any status (ACTIVE, INACTIVE, COMING_SOON).
         */
        @GetMapping
        @Operation(summary = "Browse all movies", description = "Get paginated list of all movies with optional filters. "
                        +
                        "Admins can filter by any status.")
        public ResponseEntity<ApiResponse<PagedResponse<MovieAdminResponse>>> browseMovies(
                        @ModelAttribute @Valid PagedFilterRequest<MovieFilterRequest> request) {

                Pageable pageable = request.toPageable();
                MovieFilterRequest filters = request.getFiltersOrEmpty(MovieFilterRequest::new);

                Page<Movie> movies = movieService.findAllForAdmin(pageable, filters);

                PagedResponse<MovieAdminResponse> moviesResponse = PagedResponse.of(movies,
                                movieMapper::toAdminResponse);
                return ResponseEntity.ok(
                                ApiResponse.success("Movies retrieved successfully", moviesResponse));
        }

        /**
         * Change movie status.
         * Convenience endpoint for status transitions.
         */
        @PatchMapping("/{id}/status")
        @Operation(summary = "Update movie status", description = "Update only the movie status (ACTIVE, INACTIVE, COMING_SOON)")
        public ResponseEntity<ApiResponse<MovieAdminResponse>> updateMovieStatus(
                        @PathVariable("id") UUID movieId,
                        @RequestParam MovieStatus status) {

                Movie movie = movieService.updateStatus(movieId, status);

                return ResponseEntity.ok(
                                ApiResponse.success("Movie status updated successfully",
                                                movieMapper.toAdminResponse(movie)));
        }
}