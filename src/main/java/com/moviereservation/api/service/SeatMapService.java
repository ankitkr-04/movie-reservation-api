package com.moviereservation.api.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moviereservation.api.domain.entities.Showtime;
import com.moviereservation.api.exception.ShowtimeNotFoundException;
import com.moviereservation.api.repository.SeatInstanceRepository;
import com.moviereservation.api.repository.ShowtimeRepository;
import com.moviereservation.api.repository.specification.SeatSpecification;
import com.moviereservation.api.web.dto.request.seat.SeatMapFilterRequest;
import com.moviereservation.api.web.dto.response.seat.SeatMapResponse;
import com.moviereservation.api.web.mapper.SeatMapMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatMapService {

    private final SeatInstanceRepository seatInstanceRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatMapMapper seatMapMapper;

    public SeatMapResponse getSeatMapForShowtime(final UUID showtimeId, final SeatMapFilterRequest filterRequest) {

        final Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ShowtimeNotFoundException("Showtime not found with ID: " + showtimeId));

        final var spec = SeatSpecification.withFilters(filterRequest, showtimeId);
        // Fetch seat instances based on filter criteria
        final var seatInstances = seatInstanceRepository.findAll(spec);

        return seatMapMapper.toSeatMapResponse(showtime, seatInstances);

    }

}