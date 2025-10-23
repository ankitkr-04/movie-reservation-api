package com.moviereservation.api.web.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.moviereservation.api.domain.entities.*;
import com.moviereservation.api.web.dto.response.reservation.ReservationAdminResponse;
import com.moviereservation.api.web.dto.response.reservation.ReservationCustomerResponse;

/**
 * MapStruct mapper for Reservation entity conversions.
 * Handles complex nested mappings for admin and customer views.
 */
@Mapper(componentModel = "spring")
public interface ReservationMapper {

    // ========== Customer Response Mappings ==========

    /**
     * Convert Reservation to customer response.
     * Excludes user info and administrative metadata.
     */
    @Mapping(target = "showtime", source = "reservation", qualifiedByName = "toCustomerShowtimeInfo")
    @Mapping(target = "seats", source = "reservation.reservationSeats", qualifiedByName = "toCustomerSeatDetails")
    ReservationCustomerResponse toCustomerResponse(Reservation reservation);

    @Named("toCustomerShowtimeInfo")
    default ReservationCustomerResponse.ShowtimeInfo toCustomerShowtimeInfo(final Reservation reservation) {
        final Showtime showtime = reservation.getShowtime();
        return ReservationCustomerResponse.ShowtimeInfo.builder()
                .id(showtime.getId())
                .movieTitle(showtime.getMovie().getTitle())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .screenNumber(showtime.getScreenNumber())
                .build();
    }

    @Named("toCustomerSeatDetails")
    default List<ReservationCustomerResponse.BookedSeatDetails> toCustomerSeatDetails(
            final List<ReservationSeat> reservationSeats) {
        return reservationSeats.stream()
                .map(rs -> {
                    final SeatInstance seat = rs.getSeatInstance();
                    return ReservationCustomerResponse.BookedSeatDetails.builder()
                            .seatInstanceId(seat.getId())
                            .rowLabel(seat.getRowLabel())
                            .seatNumber(seat.getSeatNumber())
                            .type(seat.getType())
                            .pricePaid(rs.getPricePaid())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ========== Admin Response Mappings ==========

    /**
     * Convert Reservation to admin response.
     * Includes full user info and administrative metadata.
     */
    @Mapping(target = "user", source = "reservation", qualifiedByName = "toUserInfo")
    @Mapping(target = "showtime", source = "reservation", qualifiedByName = "toAdminShowtimeInfo")
    @Mapping(target = "seats", source = "reservation.reservationSeats", qualifiedByName = "toAdminSeatDetails")
    ReservationAdminResponse toAdminResponse(Reservation reservation);

    @Named("toUserInfo")
    default ReservationAdminResponse.UserInfo toUserInfo(final Reservation reservation) {
        final User user = reservation.getUser();
        return ReservationAdminResponse.UserInfo.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();
    }

    @Named("toAdminShowtimeInfo")
    default ReservationAdminResponse.ShowtimeInfo toAdminShowtimeInfo(final Reservation reservation) {
        final Showtime showtime = reservation.getShowtime();
        return ReservationAdminResponse.ShowtimeInfo.builder()
                .id(showtime.getId())
                .movieTitle(showtime.getMovie().getTitle())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .screenNumber(showtime.getScreenNumber())
                .basePrice(showtime.getBasePrice())
                .build();
    }

    @Named("toAdminSeatDetails")
    default List<ReservationAdminResponse.BookedSeatDetails> toAdminSeatDetails(
            final List<ReservationSeat> reservationSeats) {
        return reservationSeats.stream()
                .map(rs -> {
                    final SeatInstance seat = rs.getSeatInstance();
                    return ReservationAdminResponse.BookedSeatDetails.builder()
                            .seatInstanceId(seat.getId())
                            .rowLabel(seat.getRowLabel())
                            .seatNumber(seat.getSeatNumber())
                            .type(seat.getType())
                            .pricePaid(rs.getPricePaid())
                            .build();
                })
                .collect(Collectors.toList());
    }
}