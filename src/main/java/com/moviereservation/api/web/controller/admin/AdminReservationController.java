package com.moviereservation.api.web.controller.admin;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.moviereservation.api.constant.Route;
import com.moviereservation.api.domain.entities.Reservation;
import com.moviereservation.api.service.ReservationService;
import com.moviereservation.api.web.dto.request.PagedFilterRequest;
import com.moviereservation.api.web.dto.request.reservation.ReservationFilterRequest;
import com.moviereservation.api.web.dto.response.reservation.ReservationAdminResponse;
import com.moviereservation.api.web.dto.response.wrappers.ApiResponse;
import com.moviereservation.api.web.dto.response.wrappers.PagedResponse;
import com.moviereservation.api.web.mapper.ReservationMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Admin reservation management endpoints.
 * Full access to all user reservations with detailed information.
 */
@RestController
@RequestMapping(Route.ADMIN + "/reservations")
@Tag(name = "Admin - Reservations", description = "Reservation management for administrators")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminReservationController {

    private final ReservationService reservationService;
    private final ReservationMapper reservationMapper;

    /**
     * Browse all reservations with filters.
     * Admin can see all users' reservations with full details including user info.
     * 
     * Available filters:
     * - statuses: Filter by reservation status
     * - userId: Filter by specific user
     * - userEmail: Search by user email (partial match)
     * - userName: Search by user name (partial match)
     * - movieId: Filter by movie
     * - movieTitle: Search by movie title (partial match)
     * - showtimeId: Filter by showtime
     * - screenNumber: Filter by screen
     * - minPrice/maxPrice: Price range
     * - createdFrom/createdTo: Reservation creation date range
     * - showtimeFrom/showtimeTo: Showtime date range
     */
    @GetMapping
    @Operation(summary = "Browse all reservations", description = "Get paginated list of all reservations with extensive filtering options. "
            +
            "Includes user information and full reservation details.")
    public ResponseEntity<ApiResponse<PagedResponse<ReservationAdminResponse>>> browseReservations(
            @ModelAttribute @Valid PagedFilterRequest<ReservationFilterRequest> request) {

        Pageable pageable = request.toPageable();
        ReservationFilterRequest filters = request.getFiltersOrEmpty(ReservationFilterRequest::new);

        Page<Reservation> reservations = reservationService.findAllForAdmin(pageable, filters);
        PagedResponse<ReservationAdminResponse> response = PagedResponse.of(reservations,
                reservationMapper::toAdminResponse);

        return ResponseEntity.ok(
                ApiResponse.success("Reservations retrieved successfully", response));
    }

    /**
     * Get reservation by booking reference.
     * Admin can access any user's reservation.
     */
    @GetMapping("/booking/{bookingReference}")
    @Operation(summary = "Get reservation by booking reference", description = "Lookup any reservation using booking reference")
    public ResponseEntity<ApiResponse<ReservationAdminResponse>> getReservationByBookingRef(
            @PathVariable @Parameter(description = "8-character booking reference", example = "ABC12345") String bookingReference) {

        Reservation reservation = reservationService.findByBookingReference(bookingReference);

        return ResponseEntity.ok(
                ApiResponse.success("Reservation retrieved successfully",
                        reservationMapper.toAdminResponse(reservation)));
    }

    /**
     * Get reservation by ID.
     * Admin can access any user's reservation.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get reservation by ID", description = "Get detailed reservation information including user details")
    public ResponseEntity<ApiResponse<ReservationAdminResponse>> getReservation(
            @PathVariable("id") UUID reservationId) {

        Reservation reservation = reservationService.findById(reservationId);

        return ResponseEntity.ok(
                ApiResponse.success("Reservation retrieved successfully",
                        reservationMapper.toAdminResponse(reservation)));
    }

    /**
     * Get all reservations for a specific user.
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get reservations by user", description = "Get all reservations for a specific user")
    public ResponseEntity<ApiResponse<PagedResponse<ReservationAdminResponse>>> getReservationsByUser(
            @PathVariable UUID userId,
            @ModelAttribute @Valid PagedFilterRequest<ReservationFilterRequest> request) {

        Pageable pageable = request.toPageable();
        ReservationFilterRequest filters = request.getFiltersOrEmpty(ReservationFilterRequest::new);

        // Override userId filter
        ReservationFilterRequest userFilter = filters.toBuilder()
                .userId(userId)
                .build();

        Page<Reservation> reservations = reservationService.findAllForAdmin(pageable, userFilter);

        PagedResponse<ReservationAdminResponse> response = PagedResponse.of(reservations,
                reservationMapper::toAdminResponse);

        return ResponseEntity.ok(
                ApiResponse.success("User reservations retrieved successfully", response));
    }

    /**
     * Get all reservations for a specific movie.
     */
    @GetMapping("/movie/{movieId}")
    @Operation(summary = "Get reservations by movie", description = "Get all reservations for a specific movie")
    public ResponseEntity<ApiResponse<PagedResponse<ReservationAdminResponse>>> getReservationsByMovie(
            @PathVariable UUID movieId,
            @ModelAttribute @Valid PagedFilterRequest<ReservationFilterRequest> request) {

        Pageable pageable = request.toPageable();
        ReservationFilterRequest filters = request.getFiltersOrEmpty(ReservationFilterRequest::new);

        // Override movieId filter
        ReservationFilterRequest movieFilter = filters.toBuilder()
                .movieId(movieId)
                .build();

        Page<Reservation> reservations = reservationService.findAllForAdmin(pageable,
                movieFilter);

        PagedResponse<ReservationAdminResponse> response = PagedResponse.of(reservations,
                reservationMapper::toAdminResponse);

        return ResponseEntity.ok(
                ApiResponse.success("Movie reservations retrieved successfully", response));
    }

    /**
     * Get all reservations for a specific showtime.
     */
    @GetMapping("/showtime/{showtimeId}")
    @Operation(summary = "Get reservations by showtime", description = "Get all reservations for a specific showtime")
    public ResponseEntity<ApiResponse<PagedResponse<ReservationAdminResponse>>> getReservationsByShowtime(
            @PathVariable UUID showtimeId,
            @ModelAttribute @Valid PagedFilterRequest<ReservationFilterRequest> request) {

        Pageable pageable = request.toPageable();
        ReservationFilterRequest filters = request.getFiltersOrEmpty(ReservationFilterRequest::new);

        // Override showtimeId filter
        ReservationFilterRequest showtimeFilter = filters.toBuilder()
                .showtimeId(showtimeId)
                .build();

        Page<Reservation> reservations = reservationService.findAllForAdmin(pageable,
                showtimeFilter);

        PagedResponse<ReservationAdminResponse> response = PagedResponse.of(reservations,
                reservationMapper::toAdminResponse);

        return ResponseEntity.ok(
                ApiResponse.success("Showtime reservations retrieved successfully", response));
    }

    /**
     * Cancel a reservation on behalf of a user.
     * Admin can cancel any reservation regardless of cancellation policy.
     */
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel reservation (Admin)", description = "Cancel any reservation. Bypasses 2-hour cancellation policy. Initiates refund.")
    public ResponseEntity<ApiResponse<ReservationAdminResponse>> cancelReservation(
            @PathVariable("id") UUID reservationId) {

        Reservation reservation = reservationService.cancelByAdmin(reservationId);

        return ResponseEntity.ok(
                ApiResponse.success("Reservation cancelled successfully. Refund initiated.",
                        reservationMapper.toAdminResponse(reservation)));
    }

    // /**
    // * Get reservation statistics.
    // * Summary of reservations by status.
    // */
    // @GetMapping("/statistics")
    // @Operation(summary = "Get reservation statistics", description = "Get summary
    // statistics of reservations (counts by status, revenue, etc.)")
    // public ResponseEntity<ApiResponse<Object>> getReservationStatistics(
    // @ModelAttribute @Valid ReservationFilterRequest filters) {

    // Object statistics = reservationService.getStatistics(filters);

    // return ResponseEntity.ok(
    // ApiResponse.success("Statistics retrieved successfully", statistics));
    // }
}