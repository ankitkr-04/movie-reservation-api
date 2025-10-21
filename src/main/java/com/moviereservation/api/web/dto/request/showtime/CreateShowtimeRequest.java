package com.moviereservation.api.web.dto.request.showtime;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShowtimeRequest {

    @NotNull(message = "Movie ID is required")
    private UUID movieId;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private Instant startTime;

    @NotNull(message = "Screen number is required")
    @Min(value = 1, message = "Screen number must be at least 1")
    @Max(value = 5, message = "Screen number cannot exceed 5")
    private Short screenNumber;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be positive")
    @Digits(integer = 8, fraction = 2, message = "Invalid price format")
    private BigDecimal basePrice;
}