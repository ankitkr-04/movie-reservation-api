package com.moviereservation.api.web.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.moviereservation.api.constant.Route;
import com.moviereservation.api.service.SeatMapService;
import com.moviereservation.api.web.dto.request.seat.SeatMapFilterRequest;
import com.moviereservation.api.web.dto.response.seat.SeatMapResponse;
import com.moviereservation.api.web.dto.response.wrappers.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Seat map viewing endpoints.
 * Shows real-time seat availability for showtimes.
 */
@RestController
@RequestMapping(Route.SHOWTIMES)
@Tag(name = "Seat Maps", description = "View seat availability for showtimes")
@RequiredArgsConstructor
public class SeatMapController {

    private final SeatMapService seatMapService;

    /**
     * Get complete seat map for a showtime.
     * Shows all seats with their current status (AVAILABLE/HELD/RESERVED).
     * 
     * Optional filters:
     * - rowLabels: Filter specific rows (e.g., "A,B,C" to show only first 3 rows)
     */
    @GetMapping("/{showtimeId}/seats")
    @Operation(summary = "Get seat map for showtime", description = "Retrieve complete seat layout with real-time availability. "
            +
            "Optionally filter by specific rows using comma-separated row labels.")
    public ResponseEntity<ApiResponse<SeatMapResponse>> getSeatMap(

            @Parameter(description = "Showtime UUID", required = true) @PathVariable UUID showtimeId,

            @ModelAttribute @Valid @Parameter(description = "Optional row filter (e.g., rowLabels=A,B,C)") SeatMapFilterRequest filterRequest) {

        SeatMapResponse seatMap = seatMapService.getSeatMap(showtimeId, filterRequest);

        return ResponseEntity.ok(
                ApiResponse.success("Seat map retrieved successfully", seatMap));
    }

    /**
     * Quick check for available seat count.
     * Lightweight endpoint without full seat details.
     */
    @GetMapping("/{showtimeId}/available-count")
    @Operation(summary = "Get available seat count", description = "Quick check for number of available seats without loading full seat map")
    public ResponseEntity<ApiResponse<Short>> getAvailableSeatsCount(
            @PathVariable UUID showtimeId) {

        Short count = seatMapService.getAvailableSeatsCount(showtimeId);

        return ResponseEntity.ok(
                ApiResponse.success("Available seats count retrieved", count));
    }
}