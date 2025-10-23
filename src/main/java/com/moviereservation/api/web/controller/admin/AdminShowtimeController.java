package com.moviereservation.api.web.controller.admin;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.moviereservation.api.constant.Route;
import com.moviereservation.api.domain.entities.Showtime;
import com.moviereservation.api.service.ShowtimeService;
import com.moviereservation.api.web.dto.request.PagedFilterRequest;
import com.moviereservation.api.web.dto.request.showtime.CreateShowtimeRequest;
import com.moviereservation.api.web.dto.request.showtime.ShowtimeFilterRequest;
import com.moviereservation.api.web.dto.request.showtime.UpdateShowtimeRequest;
import com.moviereservation.api.web.dto.response.showtime.ShowtimeAdminResponse;
import com.moviereservation.api.web.dto.response.wrappers.ApiResponse;
import com.moviereservation.api.web.dto.response.wrappers.PagedResponse;
import com.moviereservation.api.web.mapper.ShowtimeMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Admin showtime management endpoints.
 * Full control over showtime scheduling and cancellation.
 */
@RestController
@RequestMapping(Route.ADMIN + "/showtimes")
@Tag(name = "Admin - Showtimes", description = "Showtime management for administrators")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminShowtimeController {

    private final ShowtimeService showtimeService;
    private final ShowtimeMapper showtimeMapper;

    /**
     * Create a new showtime.
     * Validates no overlapping showtimes in the same screen.
     * Auto-calculates end time based on movie duration + 15min buffer.
     */
    @PostMapping
    @Operation(summary = "Create showtime", description = "Schedule a new showtime. System validates no screen conflicts and calculates end time.")
    public ResponseEntity<ApiResponse<ShowtimeAdminResponse>> createShowtime(
            @Valid @RequestBody CreateShowtimeRequest request) {

        Showtime showtime = showtimeService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Showtime created successfully",
                        showtimeMapper.toAdminResponse(showtime)));
    }

    /**
     * Update existing showtime.
     * Can only update if no reservations exist.
     * Updates to startTime will recalculate endTime.
     */
    @PatchMapping("/{id}")
    @Operation(summary = "Update showtime", description = "Update showtime details. Cannot update if reservations exist. "
            +
            "Changing start time will auto-recalculate end time.")
    public ResponseEntity<ApiResponse<ShowtimeAdminResponse>> updateShowtime(
            @PathVariable("id") UUID showtimeId,
            @Valid @RequestBody UpdateShowtimeRequest request) {

        Showtime showtime = showtimeService.update(showtimeId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Showtime updated successfully",
                        showtimeMapper.toAdminResponse(showtime)));
    }

    /**
     * Get showtime by ID (admin view with full details).
     * Can retrieve showtimes in any status including CANCELLED.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get showtime by ID", description = "Get showtime details including administrative metadata and status")
    public ResponseEntity<ApiResponse<ShowtimeAdminResponse>> getShowtime(
            @PathVariable("id") UUID showtimeId) {

        Showtime showtime = showtimeService.findById(showtimeId);

        return ResponseEntity.ok(
                ApiResponse.success("Showtime retrieved successfully",
                        showtimeMapper.toAdminResponse(showtime)));
    }

    /**
     * Cancel a showtime.
     * Automatically refunds all reservations and releases all seats.
     * Cannot be undone.
     */
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel showtime", description = "Cancel showtime and auto-refund all reservations. This action cannot be undone.")
    public ResponseEntity<ApiResponse<ShowtimeAdminResponse>> cancelShowtime(
            @PathVariable("id") UUID showtimeId) {

        Showtime showtime = showtimeService.cancel(showtimeId);

        return ResponseEntity.ok(
                ApiResponse.success("Showtime cancelled successfully. All reservations have been refunded.",
                        showtimeMapper.toAdminResponse(showtime)));
    }

    /**
     * Soft delete a showtime.
     * Can only delete future showtimes with no reservations.
     * Use cancel endpoint for showtimes with reservations.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete showtime", description = "Soft delete showtime. Can only delete future showtimes with no reservations. "
            +
            "Use cancel endpoint if reservations exist.")
    public ResponseEntity<ApiResponse<Void>> deleteShowtime(
            @PathVariable("id") UUID showtimeId) {

        showtimeService.delete(showtimeId);

        return ResponseEntity.ok(
                ApiResponse.success("Showtime deleted successfully"));
    }

    /**
     * Browse all showtimes with filters.
     * Admin can see past, future, cancelled, and completed showtimes.
     */
    @GetMapping
    @Operation(summary = "Browse all showtimes", description = "Get paginated list of all showtimes with optional filters. "
            +
            "Admins can see showtimes in any status and time period.")
    public ResponseEntity<ApiResponse<PagedResponse<ShowtimeAdminResponse>>> browseShowtimes(
            @ModelAttribute @Valid PagedFilterRequest<ShowtimeFilterRequest> request) {

        Pageable pageable = request.toPageable();
        ShowtimeFilterRequest filters = request.getFiltersOrEmpty(ShowtimeFilterRequest::new);

        Page<Showtime> showtimes = showtimeService.findAllForAdmin(pageable, filters);
        PagedResponse<ShowtimeAdminResponse> response = PagedResponse.of(showtimes,
                showtimeMapper::toAdminResponse);

        return ResponseEntity.ok(
                ApiResponse.success("Showtimes retrieved successfully", response));
    }

    /**
     * Get showtimes for a specific movie.
     * Useful for managing movie scheduling.
     */
    @GetMapping("/movie/{movieId}")
    @Operation(summary = "Get showtimes for a movie", description = "Get all showtimes for a specific movie (including past and cancelled)")
    public ResponseEntity<ApiResponse<PagedResponse<ShowtimeAdminResponse>>> getShowtimesByMovie(
            @PathVariable UUID movieId,
            @ModelAttribute @Valid PagedFilterRequest<ShowtimeFilterRequest> request) {

        Pageable pageable = request.toPageable();
        ShowtimeFilterRequest filters = request.getFiltersOrEmpty(ShowtimeFilterRequest::new);

        ShowtimeFilterRequest movieFilter = filters.toBuilder()
                .movieId(movieId)
                .build();

        Page<Showtime> showtimes = showtimeService.findAllForAdmin(pageable, movieFilter);
        PagedResponse<ShowtimeAdminResponse> response = PagedResponse.of(showtimes,
                showtimeMapper::toAdminResponse);

        return ResponseEntity.ok(
                ApiResponse.success("Showtimes for movie retrieved successfully", response));
    }

    /**
     * Get showtimes for a specific screen.
     * Useful for managing screen schedules and detecting conflicts.
     */
    @GetMapping("/screen/{screenNumber}")
    @Operation(summary = "Get showtimes for a screen", description = "Get all showtimes for a specific screen")
    public ResponseEntity<ApiResponse<PagedResponse<ShowtimeAdminResponse>>> getShowtimesByScreen(
            @PathVariable Short screenNumber,
            @ModelAttribute @Valid PagedFilterRequest<ShowtimeFilterRequest> request) {

        Pageable pageable = request.toPageable();
        ShowtimeFilterRequest filters = request.getFiltersOrEmpty(ShowtimeFilterRequest::new);

        ShowtimeFilterRequest screenFilter = filters.toBuilder()
                .screenNumber(screenNumber)
                .build();

        Page<Showtime> showtimes = showtimeService.findAllForAdmin(pageable, screenFilter);
        PagedResponse<ShowtimeAdminResponse>  response = PagedResponse.of(showtimes,
                showtimeMapper::toAdminResponse);

        return ResponseEntity.ok(
                ApiResponse.success("Showtimes for screen retrieved successfully", response));
    }
}