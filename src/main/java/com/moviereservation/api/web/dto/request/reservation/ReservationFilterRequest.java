package com.moviereservation.api.web.dto.request.reservation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moviereservation.api.domain.enums.ReservationStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Filter criteria for reservation queries.
 * Works for both admin and customer contexts:
 * - Admin: Can filter by userId, see all reservations
 * - Customer: Filtered to their own userId automatically in service layer
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ReservationFilterRequest {

    // ========== Status Filters ==========

    @Schema(description = "Filter by reservation statuses", example = "[\"CONFIRMED\", \"PENDING_PAYMENT\"]")
    private List<ReservationStatus> statuses;

    // ========== User Filter (Admin Only) ==========

    @Schema(description = "Filter by user ID (admin only, ignored for customers)", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userId;

    @Schema(description = "Search by user email (admin only, partial match)", example = "john@example.com")
    private String userEmail;

    @Schema(description = "Search by user name (admin only, partial match)", example = "John Doe")
    private String userName;

    // ========== Movie/Showtime Filters ==========

    @Schema(description = "Filter by movie ID", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID movieId;

    @Schema(description = "Search by movie title (partial match)", example = "Inception")
    private String movieTitle;

    @Schema(description = "Filter by showtime ID", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID showtimeId;

    @Schema(description = "Filter by screen number", example = "1")
    private Short screenNumber;

    // ========== Price Range ==========

    @Schema(description = "Minimum total price", example = "10.00")
    private BigDecimal minPrice;

    @Schema(description = "Maximum total price", example = "100.00")
    private BigDecimal maxPrice;

    // ========== Date Ranges ==========

    @Schema(description = "Reservation created from (inclusive)", example = "2025-01-01T00:00:00Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdFrom;

    @Schema(description = "Reservation created to (inclusive)", example = "2025-12-31T23:59:59Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdTo;

    @Schema(description = "Showtime starts from (inclusive)", example = "2025-10-01T00:00:00Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant showtimeFrom;

    @Schema(description = "Showtime starts to (inclusive)", example = "2025-10-31T23:59:59Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant showtimeTo;

    /**
     * Creates a copy without user-related filters.
     * Used to sanitize customer requests (they can only see their own).
     */
    public ReservationFilterRequest withoutUserFilters() {
        return this.toBuilder()
                .userId(null)
                .userEmail(null)
                .userName(null)
                .build();
    }

    /**
     * Check if any user-related filter is present.
     * Useful for admin endpoints to optimize queries.
     */
    public boolean hasUserFilters() {
        return userId != null ||
                (userEmail != null && !userEmail.isBlank()) ||
                (userName != null && !userName.isBlank());
    }
}