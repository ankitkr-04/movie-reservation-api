package com.moviereservation.api.web.dto.request.reservation;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class CreateReservationRequest {

    @NotNull(message = "Showtime ID is required")
    private final UUID showtimeId;

    @NotEmpty(message = "At least one seat must be selected")
    @Size(min = 1, max = 10, message = "You can reserve between 1 and 10 seats per booking")
    private final List<UUID> seatInstanceIds;
}
