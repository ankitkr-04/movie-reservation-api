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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(Route.SHOWTIMES)
@RequiredArgsConstructor
public class SeatMapController {
    private final SeatMapService seatMapService;



    @GetMapping("/{showtimeId}/seats")
    @Operation(summary = "Get seat map for a showtime")

    public ResponseEntity<ApiResponse<SeatMapResponse>> getSeatsForShowtime(
            @PathVariable final UUID showtimeId,
            @ModelAttribute @Valid final SeatMapFilterRequest filterRequest) {
        final SeatMapResponse seatMap = seatMapService.getSeatMap(showtimeId, filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Seat map fetched successfully", seatMap));

    }
}
