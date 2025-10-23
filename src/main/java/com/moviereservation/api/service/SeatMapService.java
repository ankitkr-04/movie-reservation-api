package com.moviereservation.api.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviereservation.api.domain.entities.SeatInstance;
import com.moviereservation.api.domain.entities.Showtime;
import com.moviereservation.api.exception.ShowtimeNotFoundException;
import com.moviereservation.api.repository.SeatInstanceRepository;
import com.moviereservation.api.repository.ShowtimeRepository;
import com.moviereservation.api.repository.specification.SeatSpecification;
import com.moviereservation.api.web.dto.request.seat.SeatMapFilterRequest;
import com.moviereservation.api.web.dto.response.seat.SeatMapResponse;
import com.moviereservation.api.web.mapper.SeatMapMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for seat map viewing.
 * Provides real-time seat availability for showtimes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatMapService {

    private final SeatInstanceRepository seatInstanceRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatMapMapper seatMapMapper;

    /**
     * Get complete seat map for a showtime with optional row filtering.
     * Returns all seats with real-time availability status.
     *
     * @param showtimeId    Showtime ID
     * @param filterRequest Optional row filters
     * @return Seat map response with grouped rows
     * @throws ShowtimeNotFoundException if showtime not found
     */
    @Transactional(readOnly = true)
    public SeatMapResponse getSeatMap(final UUID showtimeId, final SeatMapFilterRequest filterRequest) {
        log.debug("Fetching seat map for showtime: {}", showtimeId);

        // Validate showtime exists
        final Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ShowtimeNotFoundException(showtimeId.toString()));

        // Fetch seat instances with filters
        final List<SeatInstance> seats = seatInstanceRepository.findAll(
                SeatSpecification.withFilters(showtimeId, filterRequest));

        // Map to response
        final SeatMapResponse response = seatMapMapper.toSeatMapResponse(showtime, seats);

        log.debug("Seat map fetched: {} seats for showtime: {}", seats.size(), showtimeId);

        return response;
    }

    /**
     * Get available seats count for a showtime.
     * Lightweight endpoint for quick availability checks.
     *
     * @param showtimeId Showtime ID
     * @return Number of available seats
     * @throws ShowtimeNotFoundException if showtime not found
     */
    @Transactional(readOnly = true)
    public Short getAvailableSeatsCount(final UUID showtimeId) {
        log.debug("Fetching available seats count for showtime: {}", showtimeId);

        final Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ShowtimeNotFoundException(showtimeId.toString()));

        return showtime.getAvailableSeatsCount();
    }
}