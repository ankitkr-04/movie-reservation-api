package com.moviereservation.api.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviereservation.api.domain.entities.Movie;
import com.moviereservation.api.domain.entities.SeatInstance;
import com.moviereservation.api.domain.entities.SeatTemplate;
import com.moviereservation.api.domain.entities.Showtime;
import com.moviereservation.api.domain.enums.ReservationStatus;
import com.moviereservation.api.domain.enums.SeatStatus;
import com.moviereservation.api.domain.enums.ShowtimeStatus;
import com.moviereservation.api.exception.*;
import com.moviereservation.api.repository.ReservationRepository;
import com.moviereservation.api.repository.SeatInstanceRepository;
import com.moviereservation.api.repository.ShowtimeRepository;
import com.moviereservation.api.repository.specification.ShowtimeSpecification;
import com.moviereservation.api.web.dto.request.showtime.CreateShowtimeRequest;
import com.moviereservation.api.web.dto.request.showtime.ShowtimeFilterRequest;
import com.moviereservation.api.web.dto.request.showtime.UpdateShowtimeRequest;
import com.moviereservation.api.web.mapper.ShowtimeMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private static final int BUFFER_MINUTES = 15;
    private static final int MAX_ADVANCE_DAYS = 60;

    private final ShowtimeRepository showtimeRepository;
    private final ReservationRepository reservationRepository;
    private final SeatTemplateService seatTemplateService;
    private final SeatInstanceRepository seatInstanceRepository;
    private final MovieService movieService;
    private final ShowtimeMapper showtimeMapper;

    @Transactional
    public Showtime create(final CreateShowtimeRequest request) {
        // Validate movie exists
        final Movie movie = movieService.findById(request.getMovieId());

        // Validate scheduling constraints
        validateSchedulingConstraints(request.getStartTime());

        // Calculate end time
        final Instant endTime = calculateEndTime(request.getStartTime(), movie.getDuration());

        // Check for screen conflicts
        if (hasScreenConflict(request.getScreenNumber(), request.getStartTime(), endTime, null)) {
            throw new ShowtimeConflictException(
                    "Screen " + request.getScreenNumber() + " is already booked for this time slot");
        }

        // Create showtime
        final Showtime showtime = new Showtime();
        showtimeMapper.toEntity(request, showtime);
        showtime.setMovie(movie);
        showtime.setEndTime(endTime);
        final Showtime savedShowtime = showtimeRepository.save(showtime);

        // Create SeatInstances from SeatTemplate
        final List<SeatTemplate> templates = seatTemplateService.getTemplatesForScreen(request.getScreenNumber());

        final List<SeatInstance> seatInstances = templates.stream().map(template -> {
            final SeatInstance instance = new SeatInstance();

            instance.setShowtime(savedShowtime);
            instance.setSeatTemplate(template);
            instance.setRowLabel(template.getRowLabel());
            instance.setSeatNumber(template.getSeatNumber());
            instance.setType(template.getType());
            instance.setPrice(template.getBasePrice());
            instance.setStatus(SeatStatus.AVAILABLE);
            return instance;
        }).collect(Collectors.toList());
        // Batch Insert SeatInstances
        seatInstanceRepository.saveAll(seatInstances);

        savedShowtime.setAvailableSeatsCount((short) seatInstances.size());

        showtimeRepository.save(savedShowtime);

        return savedShowtime;
    }

    @Transactional
    public Showtime update(final UUID showtimeId, final UpdateShowtimeRequest request) {
        final Showtime showtime = findById(showtimeId);

        // Cannot update if reservations exist
        if (hasAnyReservations(showtimeId)) {
            throw new ShowtimeUpdateException(
                    "Cannot update showtime with existing reservations");
        }

        // If updating start time
        if (request.getStartTime() != null) {
            validateSchedulingConstraints(request.getStartTime());

            final Instant newEndTime = calculateEndTime(
                    request.getStartTime(),
                    showtime.getMovie().getDuration());

            final Short screenNumber = request.getScreenNumber() != null
                    ? request.getScreenNumber()
                    : showtime.getScreenNumber();

            if (hasScreenConflict(screenNumber, request.getStartTime(), newEndTime, showtimeId)) {
                throw new ShowtimeConflictException(
                        "Screen " + screenNumber + " is already booked for this time slot");
            }

            showtime.setEndTime(newEndTime);
        }

        // If updating screen number only
        if (request.getScreenNumber() != null && request.getStartTime() == null) {
            if (hasScreenConflict(request.getScreenNumber(), showtime.getStartTime(), showtime.getEndTime(),
                    showtimeId)) {
                throw new ShowtimeConflictException(
                        "Screen " + request.getScreenNumber() + " is already booked for this time slot");
            }
        }

        showtimeMapper.toEntity(request, showtime);
        return showtimeRepository.save(showtime);
    }

    @Transactional
    public void cancel(final UUID showtimeId) {
        final Showtime showtime = findById(showtimeId);

        if (showtime.getStatus() == ShowtimeStatus.CANCELLED) {
            throw new ShowtimeAlreadyCancelledException("Showtime is already cancelled");
        }

        if (showtime.getStatus() == ShowtimeStatus.COMPLETED) {
            throw new ShowtimeCancellationException("Cannot cancel completed showtime");
        }

        showtime.setStatus(ShowtimeStatus.CANCELLED);
        showtimeRepository.save(showtime);

        // TODO: Implement refund logic when payment is added
        // For now, just mark reservations as cancelled
        // refundAllReservations(showtimeId);
    }

    @Transactional
    public void deleteById(final UUID showtimeId) {
        final Showtime showtime = findById(showtimeId);

        // Cannot delete past showtimes
        if (showtime.getStartTime().isBefore(Instant.now())) {
            throw new ShowtimeDeletionException("Cannot delete past showtimes");
        }

        // Cannot delete if has reservations
        if (hasAnyReservations(showtimeId)) {
            throw new ShowtimeDeletionException(
                    "Cannot delete showtime with existing reservations. Please cancel the showtime instead.");
        }

        showtimeRepository.delete(showtime);
    }

    @Transactional(readOnly = true)
    public Showtime findById(final UUID showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ShowtimeNotFoundException(showtimeId.toString()));
    }

    @Transactional(readOnly = true)
    public Page<Showtime> findAllForAdmin(final Pageable pageable, final ShowtimeFilterRequest filters) {
        return showtimeRepository.findAll(
                ShowtimeSpecification.forAdmin(filters),
                pageable);
    }

    @Transactional(readOnly = true)
    public Page<Showtime> findAllForCustomer(final Pageable pageable, final ShowtimeFilterRequest filters) {
        return showtimeRepository.findAll(
                ShowtimeSpecification.forCustomer(filters),
                pageable);
    }

    // ========== Private Helper Methods ==========

    private Instant calculateEndTime(final Instant startTime, final Integer movieDuration) {
        return startTime.plus(movieDuration + BUFFER_MINUTES, ChronoUnit.MINUTES);
    }

    private void validateSchedulingConstraints(final Instant startTime) {
        final Instant now = Instant.now();
        final Instant maxAdvanceDate = now.plus(MAX_ADVANCE_DAYS, ChronoUnit.DAYS);

        if (startTime.isBefore(now)) {
            throw new InvalidShowtimeException("Cannot schedule showtime in the past");
        }

        if (startTime.isAfter(maxAdvanceDate)) {
            throw new InvalidShowtimeException(
                    "Cannot schedule showtime more than " + MAX_ADVANCE_DAYS + " days in advance");
        }
    }

    private boolean hasScreenConflict(
            final Short screenNumber,
            final Instant startTime,
            final Instant endTime,
            final UUID excludeShowtimeId) {

        return showtimeRepository.existsConflictingShowtime(
                screenNumber,
                startTime,
                endTime,
                excludeShowtimeId);
    }

    private boolean hasAnyReservations(final UUID showtimeId) {
        return reservationRepository.existsByShowtimeIdAndStatusIn(
                showtimeId,
                ReservationStatus.CONFIRMED,
                ReservationStatus.PENDING_PAYMENT);
    }

}