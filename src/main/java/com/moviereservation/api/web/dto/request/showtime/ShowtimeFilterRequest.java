package com.moviereservation.api.web.dto.request.showtime;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.moviereservation.api.domain.enums.ShowtimeStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeFilterRequest {

    @Schema(description = "Filter by movie ID")
    private UUID movieId;

    @Schema(description = "Filter by screen number")
    private Short screenNumber;

    @Schema(description = "Start time from (inclusive)")
    private Instant startTimeFrom;

    @Schema(description = "Start time to (inclusive)")
    private Instant startTimeTo;

    @Schema(description = "Filter by statuses")
    private List<ShowtimeStatus> statuses;

    public ShowtimeFilterRequest withoutStatuses() {
        return this.toBuilder()
                .statuses(null)
                .build();
    }
}