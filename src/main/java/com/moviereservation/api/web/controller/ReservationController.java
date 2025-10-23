package com.moviereservation.api.web.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.moviereservation.api.constant.Route;
import com.moviereservation.api.domain.entities.Reservation;
import com.moviereservation.api.security.UserPrincipal;
import com.moviereservation.api.service.ReservationService;
import com.moviereservation.api.web.dto.request.PagedFilterRequest;
import com.moviereservation.api.web.dto.request.reservation.CreateReservationRequest;
import com.moviereservation.api.web.dto.request.reservation.ReservationFilterRequest;
import com.moviereservation.api.web.dto.response.reservation.ReservationCustomerResponse;
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
 * Customer reservation management endpoints.
 * Users can only view and manage their own reservations.
 */
@RestController
@RequestMapping(Route.RESERVATIONS)
@Tag(name = "Reservations (Customer)", description = "Manage your own reservations")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ReservationController {

        private final ReservationService reservationService;
        private final ReservationMapper reservationMapper;

        /**
         * Create a new reservation (book seats).
         * Seats are held for 5 minutes pending payment.
         */
        @PostMapping
        @Operation(summary = "Create reservation", description = "Book seats for a showtime. Seats will be held for 5 minutes pending payment. "
                        +
                        "Returns PENDING_PAYMENT status with booking reference.")
        public ResponseEntity<ApiResponse<ReservationCustomerResponse>> createReservation(
                        @Valid @RequestBody final CreateReservationRequest request,
                        @AuthenticationPrincipal final UserPrincipal principal) {

                final UUID userId = principal.getUserId();
                final Reservation reservation = reservationService.create(userId, request);

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(
                                                "Reservation created successfully. Complete payment within 5 minutes.",
                                                reservationMapper.toCustomerResponse(reservation)));
        }

        /**
         * Get all user's own reservations with filters.
         * Automatically scoped to authenticated user.
         */
        @GetMapping
        @Operation(summary = "Get my reservations", description = "Retrieve paginated list of your own reservations with optional filters "
                        +
                        "(status, movie, showtime, date ranges, price range)")
        public ResponseEntity<ApiResponse<PagedResponse<ReservationCustomerResponse>>> getMyReservations(
                        @ModelAttribute @Valid final PagedFilterRequest<ReservationFilterRequest> request,
                        @AuthenticationPrincipal final UserPrincipal principal) {

                final Pageable pageable = request.toPageable();
                final ReservationFilterRequest filters = request.getFiltersOrEmpty(ReservationFilterRequest::new);
                final UUID userId = principal.getUserId();

                final Page<Reservation> reservations = reservationService.findAllForCustomer(userId,
                                pageable, filters);

                final PagedResponse<ReservationCustomerResponse> reservationsResponse = PagedResponse.of(
                                reservations, reservationMapper::toCustomerResponse);

                return ResponseEntity.ok(
                                ApiResponse.success("Reservations retrieved successfully", reservationsResponse));
        }

        /**
         * Get single reservation by booking reference.
         * User can only access their own reservations.
         */
        @GetMapping("/{bookingReference}")
        @Operation(summary = "Get reservation by booking reference", description = "Get detailed reservation information using booking reference (e.g., 'ABC12345')")
        public ResponseEntity<ApiResponse<ReservationCustomerResponse>> getReservation(
                        @PathVariable @Parameter(description = "8-character booking reference", example = "ABC12345") final String bookingReference,
                        @AuthenticationPrincipal final UserPrincipal principal) {

                final UUID userId = principal.getUserId();
                final Reservation reservation = reservationService
                                .findByBookingReferenceAndUser(bookingReference, userId);

                return ResponseEntity.ok(
                                ApiResponse.success("Reservation retrieved successfully",
                                                reservationMapper.toCustomerResponse(reservation)));
        }

        /**
         * Cancel a reservation.
         * Must cancel at least 2 hours before showtime.
         * Can only cancel CONFIRMED reservations.
         */
        @PostMapping("/{bookingReference}/cancel")
        @Operation(summary = "Cancel reservation", description = "Cancel a CONFIRMED reservation. Must be at least 2 hours before showtime. "
                        +
                        "Refund will be initiated automatically.")
        public ResponseEntity<ApiResponse<ReservationCustomerResponse>> cancelReservation(
                        @PathVariable final String bookingReference,
                        @AuthenticationPrincipal final UserPrincipal principal) {

                final UUID userId = principal.getUserId();
                final Reservation reservation = reservationService.cancel(bookingReference, userId);

                return ResponseEntity.ok(
                                ApiResponse.success("Reservation cancelled successfully. Refund will be processed.",
                                                reservationMapper.toCustomerResponse(reservation)));
        }

        /**
         * Get reservation by ID (alternative to booking reference).
         * User can only access their own reservations.
         */
        @GetMapping("/id/{bookingReference}")
        @Operation(summary = "Get reservation by ID", description = "Get reservation using UUID (alternative to booking reference)")
        public ResponseEntity<ApiResponse<ReservationCustomerResponse>> getReservationById(
                        @PathVariable("bookingReference") final String bookingReference,
                        @AuthenticationPrincipal final UserPrincipal principal) {

                final UUID userId = principal.getUserId();
                final Reservation reservation = reservationService.findByBookingReferenceAndUser(
                                bookingReference,
                                userId);

                return ResponseEntity.ok(
                                ApiResponse.success("Reservation retrieved successfully",
                                                reservationMapper.toCustomerResponse(reservation)));
        }
}