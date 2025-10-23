package com.moviereservation.api.web.mapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.moviereservation.api.domain.entities.SeatInstance;
import com.moviereservation.api.domain.entities.Showtime;
import com.moviereservation.api.domain.enums.SeatStatus;
import com.moviereservation.api.web.dto.response.seat.SeatAvailabilityResponse;
import com.moviereservation.api.web.dto.response.seat.SeatMapResponse;

/**
 * MapStruct mapper for Seat entity conversions.
 * Maps seat instances to availability responses with proper enum handling.
 */
@Mapper(componentModel = "spring")
public interface SeatMapMapper {

    /**
     * Map Showtime and its seats to SeatMapResponse.
     * Groups seats by row for structured response.
     */
    @Mapping(target = "showtimeId", source = "showtime.id")
    @Mapping(target = "screenNumber", source = "showtime.screenNumber")
    @Mapping(target = "showtimeBasePrice", source = "showtime.basePrice")
    @Mapping(target = "rows", source = "seats", qualifiedByName = "toRowsList")
    SeatMapResponse toSeatMapResponse(Showtime showtime, List<SeatInstance> seats);

    /**
     * Map single SeatInstance to SeatAvailabilityResponse.
     * Uses enums directly instead of string codes.
     */
    @Mapping(target = "seatInstanceId", source = "id")
    @Mapping(target = "rowLabel", source = "rowLabel")
    @Mapping(target = "number", source = "seatNumber")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "totalPrice", source = "price")
    @Mapping(target = "heldUntil", source = ".", qualifiedByName = "calculateHeldUntil")
    SeatAvailabilityResponse toSeatAvailabilityResponse(SeatInstance seat);

    /**
     * Group seats by row and convert to list of SeatRow objects.
     * Maintains row order (A-J) and seat number order within each row.
     */
    @Named("toRowsList")
    default List<SeatMapResponse.SeatRow> toRowsList(final List<SeatInstance> seats) {
        return seats.stream()
                .collect(Collectors.groupingBy(SeatInstance::getRowLabel))
                .entrySet().stream()
                .sorted(Comparator.comparing(Entry::getKey))
                .map(entry -> {
                    final List<SeatAvailabilityResponse> sortedSeats = entry.getValue().stream()
                            .sorted(Comparator.comparing(SeatInstance::getSeatNumber))
                            .map(this::toSeatAvailabilityResponse)
                            .collect(Collectors.toList());

                    return SeatMapResponse.SeatRow.builder()
                            .rowLabel(entry.getKey())
                            .seats(sortedSeats)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Calculate when a HELD seat will be released.
     * Returns heldAt + 5 minutes for HELD seats, null otherwise.
     */
    @Named("calculateHeldUntil")
    default Instant calculateHeldUntil(final SeatInstance seat) {
        if (seat.getStatus() == SeatStatus.HELD && seat.getHeldAt() != null) {
            return seat.getHeldAt().plus(Duration.ofMinutes(5));
        }
        return null;
    }
}