package com.moviereservation.api.web.controller.admin;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.moviereservation.api.constant.Route;
import com.moviereservation.api.service.AnalyticsService;
import com.moviereservation.api.web.dto.response.wrappers.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Admin analytics and reporting endpoints.
 * Provides revenue, occupancy, and performance metrics.
 */
@RestController
@RequestMapping(Route.ADMIN + "/analytics")
@Tag(name = "Admin - Analytics", description = "Revenue and performance analytics")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Get revenue summary (all-time).
     */
    @GetMapping("/revenue/summary")
    @Operation(summary = "Get revenue summary", description = "Returns total revenue, bookings, and customer metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRevenueSummary() {
        final Map<String, Object> summary = analyticsService.getRevenueSummary();
        return ResponseEntity.ok(ApiResponse.success("Revenue summary retrieved", summary));
    }

    /**
     * Get revenue by date range.
     */
    @GetMapping("/revenue/date-range")
    @Operation(summary = "Get revenue by date range", description = "Returns aggregated revenue metrics for a specific period")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRevenueByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "Start date (YYYY-MM-DD)", example = "2025-01-01") final LocalDate startDate,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "End date (YYYY-MM-DD)", example = "2025-12-31") final LocalDate endDate) {

        final Map<String, Object> revenue = analyticsService.getRevenueByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Revenue data retrieved", revenue));
    }

    /**
     * Get daily revenue breakdown.
     */
    @GetMapping("/revenue/daily")
    @Operation(summary = "Get daily revenue", description = "Returns day-by-day revenue breakdown for a period")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDailyRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate endDate) {

        final List<Map<String, Object>> dailyRevenue = analyticsService.getDailyRevenue(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Daily revenue retrieved", dailyRevenue));
    }

    /**
     * Get revenue by genre.
     */
    @GetMapping("/revenue/by-genre")
    @Operation(summary = "Get revenue by genre", description = "Returns revenue breakdown by movie genre")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRevenueByGenre() {
        final List<Map<String, Object>> revenueByGenre = analyticsService.getRevenueByGenre();
        return ResponseEntity.ok(ApiResponse.success("Revenue by genre retrieved", revenueByGenre));
    }

    /**
     * Get top movies by revenue.
     */
    @GetMapping("/movies/top-revenue")
    @Operation(summary = "Get top movies by revenue", description = "Returns most profitable movies")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopMoviesByRevenue(
            @RequestParam(defaultValue = "10") @Parameter(description = "Number of movies to return", example = "10") final int limit) {

        final List<Map<String, Object>> topMovies = analyticsService.getTopMoviesByRevenue(limit);
        return ResponseEntity.ok(ApiResponse.success("Top movies retrieved", topMovies));
    }

    /**
     * Get movie performance metrics.
     */
    @GetMapping("/movies/performance")
    @Operation(summary = "Get movie performance", description = "Returns booking and revenue metrics for all movies")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMoviePerformance() {
        final List<Map<String, Object>> performance = analyticsService.getMoviePerformance();
        return ResponseEntity.ok(ApiResponse.success("Movie performance retrieved", performance));
    }

    /**
     * Get showtime occupancy rates.
     */
    @GetMapping("/occupancy")
    @Operation(summary = "Get showtime occupancy", description = "Returns occupancy rates for showtimes on a specific date")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getShowtimeOccupancy(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "Date (YYYY-MM-DD)", example = "2025-10-25") final LocalDate date) {

        final List<Map<String, Object>> occupancy = analyticsService.getShowtimeOccupancy(date);
        return ResponseEntity.ok(ApiResponse.success("Occupancy data retrieved", occupancy));
    }

    /**
     * Get occupancy statistics.
     */
    @GetMapping("/occupancy/stats")
    @Operation(summary = "Get occupancy statistics", description = "Returns average, min, max occupancy rates")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOccupancyStats() {
        final Map<String, Object> stats = analyticsService.getOccupancyStats();
        return ResponseEntity.ok(ApiResponse.success("Occupancy stats retrieved", stats));
    }

    /**
     * Get peak booking hours.
     */
    @GetMapping("/bookings/peak-hours")
    @Operation(summary = "Get peak booking hours", description = "Returns booking patterns by hour of day (0-23)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPeakBookingHours() {
        final List<Map<String, Object>> peakHours = analyticsService.getPeakBookingHours();
        return ResponseEntity.ok(ApiResponse.success("Peak hours retrieved", peakHours));
    }

    /**
     * Manually refresh analytics views.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh analytics", description = "Manually refreshes all analytics materialized views. " +
            "Useful after bulk data changes. Normally runs daily at 1 AM.")
    public ResponseEntity<ApiResponse<Void>> refreshAnalytics() {
        analyticsService.refreshAnalyticsViews();
        return ResponseEntity.ok(ApiResponse.success("Analytics refreshed successfully"));
    }
}