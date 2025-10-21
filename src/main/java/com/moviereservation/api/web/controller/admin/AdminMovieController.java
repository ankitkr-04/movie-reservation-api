package com.moviereservation.api.web.controller.admin;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.moviereservation.api.constant.Route;
import com.moviereservation.api.domain.entities.Movie;
import com.moviereservation.api.service.MovieService;
import com.moviereservation.api.web.dto.request.PagedFilterRequest;
import com.moviereservation.api.web.dto.request.movie.CreateMovieRequest;
import com.moviereservation.api.web.dto.request.movie.FilterMovieRequest;
import com.moviereservation.api.web.dto.request.movie.UpdateMovieRequest;
import com.moviereservation.api.web.dto.response.movie.MovieResponse;
import com.moviereservation.api.web.dto.response.wrappers.ApiResponse;
import com.moviereservation.api.web.dto.response.wrappers.PagedResponse;
import com.moviereservation.api.web.mapper.MovieMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(Route.ADMIN_MOVIES)
@Tag(name = "Admin - Movies", description = "Movie management for administrators")
@RequiredArgsConstructor
public class AdminMovieController {
        private final MovieService movieService;
        private final MovieMapper movieMapper;

        @PostMapping
        public ResponseEntity<ApiResponse<MovieResponse>> createMovie(
                        @Valid @RequestBody final CreateMovieRequest request) {

                final Movie movie = movieService.createMovie(request);

                final MovieResponse movieResponse = movieMapper.toAdminResponse(movie);

                return ResponseEntity.ok(
                                ApiResponse.success("Movie created successfully", movieResponse));

        }

        @PatchMapping("/{movieId}")
        public ResponseEntity<ApiResponse<MovieResponse>> updateMovie(
                        @PathVariable final UUID movieId,
                        @Valid @RequestBody final UpdateMovieRequest request) {
                final Movie movie = movieService.updateMovie(movieId, request);
                final MovieResponse movieResponse = movieMapper.toAdminResponse(movie);
                return ResponseEntity.ok(
                                ApiResponse.success("Movie updated successfully", movieResponse));
        }

        @GetMapping("/{movieId}")
        public ResponseEntity<ApiResponse<MovieResponse>> getMovie(
                        @PathVariable final UUID movieId) {
                final Movie movie = movieService.getMovieById(movieId);
                final MovieResponse movieResponse = movieMapper.toAdminResponse(movie);
                return ResponseEntity.ok(
                                ApiResponse.success("Movie fetched successfully", movieResponse));
        }

        @GetMapping
        @Operation(summary = "Get all movies (Admin)", description = "Retrieve a paginated list of movies with optional filtering. Admins can filter by any status.")
        public ResponseEntity<ApiResponse<PagedResponse<MovieResponse>>> getMovies(
                        @ModelAttribute final PagedFilterRequest<FilterMovieRequest> pagedFilterRequest) {

                final Pageable pageable = pagedFilterRequest.toPageable();
                final FilterMovieRequest filters = pagedFilterRequest.getFiltersOrEmpty(FilterMovieRequest::new);

                final var pagedMovies = movieService.getAllMoviesForAdmin(pageable, filters);
                final var pagedMovieResponses = PagedResponse.of(pagedMovies, movieMapper::toAdminResponse);

                return ResponseEntity.ok(
                                ApiResponse.success("Movies fetched successfully", pagedMovieResponses));

        }

}
