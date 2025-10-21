package com.moviereservation.api.web.dto.request.showtime;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShowtimeRequest {

    @Future(message = "Start time must be in the future")
    private Instant startTime;

    @Min(value = 1, message = "Screen number must be at least 1")
    @Max(value = 5, message = "Screen number cannot exceed 5")
    private Short screenNumber;

    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be positive")
    @Digits(integer = 8, fraction = 2, message = "Invalid price format")
    private BigDecimal basePrice;
}