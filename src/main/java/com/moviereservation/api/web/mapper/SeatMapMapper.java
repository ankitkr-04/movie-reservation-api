package com.moviereservation.api.web.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import com.moviereservation.api.domain.entities.SeatInstance;
import com.moviereservation.api.domain.entities.Showtime;
import com.moviereservation.api.domain.enums.SeatStatus;
import com.moviereservation.api.domain.enums.SeatType;
import com.moviereservation.api.web.dto.response.seat.SeatMapResponse;
import com.moviereservation.api.web.dto.response.seat.SeatResponse;

@Mapper(componentModel = "spring")
public interface SeatMapMapper {

    SeatMapMapper INSTANCE = Mappers.getMapper(SeatMapMapper.class);

    @Mapping(target = "showtimeId", source = "showtime.id")
    @Mapping(target = "screenNumber", source = "showtime.screenNumber")
    @Mapping(target = "showtimeBasePrice", source = "showtime.basePrice")
    @Mapping(target = "rows", source = "seats", qualifiedByName = "toRowsMap")
    SeatMapResponse toSeatMapResponse(Showtime showtime, List<SeatInstance> seats);

    @Mapping(target = "number", source = "seatNumber")
    @Mapping(target = "status", source = "status", qualifiedByName = "toStatusCode")
    @Mapping(target = "type", source = "type", qualifiedByName = "toTypeCode")
    @Mapping(target = "totalPrice", source = ".", qualifiedByName = "toTotalPrice")
    @Mapping(target = "heldUntil", source = "heldAt", qualifiedByName = "toHeldUntil")
    SeatResponse toSeatResponse(SeatInstance seat);

    @Named("toRowsMap")
    default Map<String, SeatResponse[]> toRowsMap(final List<SeatInstance> seats) {
        return seats.stream()
                .collect(Collectors.groupingBy(
                        seat -> seat.getRowLabel().toString(),
                        Collectors.mapping(INSTANCE::toSeatResponse, Collectors.toList())))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().toArray(new SeatResponse[0])));
    }

    @Named("toStatusCode")
    default String toStatusCode(final SeatStatus status) {
        return switch (status) {
            case AVAILABLE -> "A";
            case HELD -> "H";
            case RESERVED -> "R";
        };
    }

    @Named("toTypeCode")
    default String toTypeCode(final SeatType type) {
        return switch (type) {
            case REGULAR -> "R";
            case PREMIUM -> "P";
        };
    }

    @Named("toTotalPrice")
    default BigDecimal toTotalPrice(final SeatInstance seat) {
        final BigDecimal showtimeBasePrice = seat.getShowtime().getBasePrice();
        return showtimeBasePrice.add(seat.getPrice());
    }

    @Named("toHeldUntil")
    default Instant toHeldUntil(final Instant heldAt) {
        return heldAt != null && SeatStatus.HELD.equals(heldAt) ? heldAt : null;
    }
}