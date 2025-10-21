package com.moviereservation.api.web.controller.admin;

import java.util.UUID;

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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(Route.ADMIN + "/showtimes")
@Tag(name = "Admin - Showtimes", description = "Showtime management for administrators")
@RequiredArgsConstructor
public class AdminShowtimeController {

    private final ShowtimeService showtimeService;
    private final ShowtimeMapper showtimeMapper;

    @PostMapping
    @Operation(summary = "Create new showtime")
    public ResponseEntity<ApiResponse<ShowtimeAdminResponse>> createShowtime(
            @Valid @RequestBody final CreateShowtimeRequest request) {

        final Showtime showtime = showtimeService.create(request);
        final ShowtimeAdminResponse response = showtimeMapper.toAdminResponse(showtime);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Showtime created successfully", response));
    }

    @PatchMapping("/{showtimeId}")
    @Operation(summary = "Update existing showtime")
    public ResponseEntity<ApiResponse<ShowtimeAdminResponse>> updateShowtime(
            @PathVariable final UUID showtimeId,
            @Valid @RequestBody final UpdateShowtimeRequest request) {

        final Showtime showtime = showtimeService.update(showtimeId, request);
        final ShowtimeAdminResponse response = showtimeMapper.toAdminResponse(showtime);

        return ResponseEntity.ok(ApiResponse.success("Showtime updated successfully", response));
    }

    @PostMapping("/{showtimeId}/cancel")
    @Operation(summary = "Cancel showtime", description = "Cancels showtime and auto-refunds all reservations")
    public ResponseEntity<ApiResponse<Void>> cancelShowtime(
            @PathVariable final UUID showtimeId) {

        showtimeService.cancel(showtimeId);
        return ResponseEntity.ok(ApiResponse.success("Showtime cancelled successfully", null));
    }

    @DeleteMapping("/{showtimeId}")
    @Operation(summary = "Delete showtime", description = "Can only delete future showtimes with no reservations")
    public ResponseEntity<ApiResponse<Void>> deleteShowtime(
            @PathVariable final UUID showtimeId) {

        showtimeService.deleteById(showtimeId);
        return ResponseEntity.ok(ApiResponse.success("Showtime deleted successfully", null));
    }

    @GetMapping("/{showtimeId}")
    @Operation(summary = "Get showtime by ID")
    public ResponseEntity<ApiResponse<ShowtimeAdminResponse>> getShowtime(
            @PathVariable final UUID showtimeId) {

        final Showtime showtime = showtimeService.findById(showtimeId);
        final ShowtimeAdminResponse response = showtimeMapper.toAdminResponse(showtime);

        return ResponseEntity.ok(ApiResponse.success("Showtime fetched successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all showtimes (Admin)")
    public ResponseEntity<ApiResponse<PagedResponse<ShowtimeAdminResponse>>> getShowtimes(
            @ModelAttribute final PagedFilterRequest<ShowtimeFilterRequest> pagedFilterRequest) {

        final Pageable pageable = pagedFilterRequest.toPageable();
        final ShowtimeFilterRequest filters = pagedFilterRequest.getFiltersOrEmpty(ShowtimeFilterRequest::new);

        final var pagedShowtimes = showtimeService.findAllForAdmin(pageable, filters);
        final var pagedResponses = PagedResponse.of(pagedShowtimes, showtimeMapper::toAdminResponse);

        return ResponseEntity.ok(ApiResponse.success("Showtimes fetched successfully", pagedResponses));
    }
}