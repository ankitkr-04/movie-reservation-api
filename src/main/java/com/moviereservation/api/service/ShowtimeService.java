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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for showtime management.
 * Handles showtime scheduling, validation, and seat instance creation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeService {

    // Constants
    private static final int BUFFER_MINUTES = 15;
    private static final int MAX_ADVANCE_DAYS = 60;

    private final ShowtimeRepository showtimeRepository;
    private final ReservationRepository reservationRepository;
    private final SeatInstanceRepository seatInstanceRepository;
    private final SeatTemplateService seatTemplateService;
    private final MovieService movieService;
    private final ShowtimeMapper showtimeMapper;

    /**
     * Create a new showtime with seat instances.
     * Validates scheduling constraints and screen conflicts.
     *
     * @param request Showtime creation details
     * @return Created showtime entity
     * @throws InvalidShowtimeException  if scheduling constraints violated
     * @throws ShowtimeConflictException if screen is already booked
     */
    @Transactional
    public Showtime create(final CreateShowtimeRequest request) {
        log.debug("Creating showtime for movie: {} at screen: {}",
                request.getMovieId(), request.getScreenNumber());

        // Validate movie exists
        final Movie movie = movieService.findById(request.getMovieId());

        // Validate scheduling constraints
        validateSchedulingConstraints(request.getStartTime());

        // Calculate end time
        final Instant endTime = calculateEndTime(request.getStartTime(), movie.getDuration());

        // Check screen conflicts
        validateNoScreenConflict(request.getScreenNumber(), request.getStartTime(), endTime, null);

        // Create showtime entity
        final Showtime showtime = showtimeMapper.toEntity(request);
        showtime.setMovie(movie);
        showtime.setEndTime(endTime);
        showtime.setStatus(ShowtimeStatus.SCHEDULED);

        final Showtime savedShowtime = showtimeRepository.save(showtime);

        // Create seat instances from templates
        createSeatInstances(savedShowtime);

        log.info("Showtime created: {} for movie: {} at screen: {}",
                savedShowtime.getId(), movie.getTitle(), request.getScreenNumber());

        return savedShowtime;
    }

    /**
     * Update an existing showtime.
     * Cannot update if reservations exist.
     * Recalculates end time if start time is changed.
     *
     * @param showtimeId Showtime ID
     * @param request    Update details
     * @return Updated showtime entity
     * @throws ShowtimeUpdateException   if showtime has reservations
     * @throws ShowtimeConflictException if screen conflict detected
     */
    @Transactional
    public Showtime update(final UUID showtimeId, final UpdateShowtimeRequest request) {
        log.debug("Updating showtime: {}", showtimeId);

        final Showtime showtime = findById(showtimeId);

        // Cannot update if reservations exist
        validateNoReservations(showtimeId);

        // Handle start time update
        if (request.getStartTime() != null) {
            validateSchedulingConstraints(request.getStartTime());

            final Instant newEndTime = calculateEndTime(request.getStartTime(), showtime.getMovie().getDuration());
            final Short screenNumber = request.getScreenNumber() != null
                    ? request.getScreenNumber()
                    : showtime.getScreenNumber();

            validateNoScreenConflict(screenNumber, request.getStartTime(), newEndTime, showtimeId);

            showtime.setEndTime(newEndTime);
        }

        // Handle screen number update
        if (request.getScreenNumber() != null && request.getStartTime() == null) {
            validateNoScreenConflict(
                    request.getScreenNumber(),
                    showtime.getStartTime(),
                    showtime.getEndTime(),
                    showtimeId);
        }

        // Apply updates
        showtimeMapper.updateEntity(request, showtime);
        final Showtime updatedShowtime = showtimeRepository.save(showtime);

        log.info("Showtime updated: {}", showtimeId);
        return updatedShowtime;
    }

    /**
     * Cancel a showtime.
     * Marks showtime as CANCELLED and triggers refunds for all reservations.
     *
     * @param showtimeId Showtime ID
     * @return Cancelled showtime entity
     * @throws ShowtimeAlreadyCancelledException if already cancelled
     * @throws ShowtimeCancellationException     if cannot be cancelled
     */
    @Transactional
    public Showtime cancel(final UUID showtimeId) {
        log.debug("Cancelling showtime: {}", showtimeId);

        final Showtime showtime = findById(showtimeId);

        // Validate can be cancelled
        validateCanBeCancelled(showtime);

        // Mark as cancelled
        showtime.setStatus(ShowtimeStatus.CANCELLED);
        final Showtime cancelledShowtime = showtimeRepository.save(showtime);

        // TODO: Trigger refund process when payment is implemented
        // refundService.refundAllReservations(showtimeId);

        log.info("Showtime cancelled: {}", showtimeId);
        return cancelledShowtime;
    }

    /**
     * Soft delete a showtime.
     * Can only delete future showtimes with no reservations.
     *
     * @param showtimeId Showtime ID
     * @throws ShowtimeDeletionException if deletion constraints violated
     */
    @Transactional
    public void delete(final UUID showtimeId) {
        log.debug("Deleting showtime: {}", showtimeId);

        final Showtime showtime = findById(showtimeId);

        // Validate can be deleted
        validateCanBeDeleted(showtime);

        showtimeRepository.delete(showtime);

        log.info("Showtime deleted: {}", showtimeId);
    }

    /**
     * Find showtime by ID (all access).
     *
     * @throws ShowtimeNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public Showtime findById(final @NonNull UUID showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ShowtimeNotFoundException(showtimeId.toString()));
    }

    /**
     * Find showtime by ID (customer access - future SCHEDULED only).
     *
     * @throws ShowtimeNotFoundException if not found or not customer-visible
     */
    @Transactional(readOnly = true)
    public Showtime findByIdForCustomer(final UUID showtimeId) {
        return showtimeRepository.findOne(
                ShowtimeSpecification.isNotDeleted()
                        .and(ShowtimeSpecification.isFuture())
                        .and(ShowtimeSpecification.isScheduled())
                        .and((root, _, cb) -> cb.equal(root.get("id"), showtimeId)))
                .orElseThrow(() -> new ShowtimeNotFoundException(showtimeId.toString()));
    }

    /**
     * Find all showtimes for admin with filters and pagination.
     * Includes all statuses and time periods.
     */
    @Transactional(readOnly = true)
    public Page<Showtime> findAllForAdmin(final Pageable pageable, final ShowtimeFilterRequest filters) {
        log.debug("Finding showtimes for admin with filters");

        return showtimeRepository.findAll(
                ShowtimeSpecification.forAdmin(filters),
                pageable);
    }

    /**
     * Find all showtimes for customer with filters and pagination.
     * Auto-restricts to future SCHEDULED showtimes.
     */
    @Transactional(readOnly = true)
    public Page<Showtime> findAllForCustomer(final Pageable pageable, final ShowtimeFilterRequest filters) {
        log.debug("Finding showtimes for customer with filters");

        return showtimeRepository.findAll(
                ShowtimeSpecification.forCustomer(filters),
                pageable);
    }

    // ========== Private Helper Methods ==========

    /**
     * Calculate showtime end time: start time + movie duration + buffer.
     */
    private Instant calculateEndTime(final Instant startTime, final Integer movieDuration) {
        return startTime.plus(movieDuration + BUFFER_MINUTES, ChronoUnit.MINUTES);
    }

    /**
     * Create seat instances from templates for a showtime.
     */
    private void createSeatInstances(final Showtime showtime) {
        final List<SeatTemplate> templates = seatTemplateService.getTemplatesForScreen(showtime.getScreenNumber());

        final List<SeatInstance> seatInstances = templates.stream()
                .map(template -> createSeatInstance(template, showtime))
                .collect(Collectors.toList());

        seatInstanceRepository.saveAll(seatInstances);

        // Update available seats count
        showtime.setAvailableSeatsCount((short) seatInstances.size());
        showtimeRepository.save(showtime);

        log.debug("Created {} seat instances for showtime: {}", seatInstances.size(), showtime.getId());
    }

    /**
     * Create a single seat instance from template.
     */
    private SeatInstance createSeatInstance(final SeatTemplate template, final Showtime showtime) {
        final SeatInstance instance = new SeatInstance();
        instance.setShowtime(showtime);
        instance.setSeatTemplate(template);
        instance.setRowLabel(template.getRowLabel());
        instance.setSeatNumber(template.getSeatNumber());
        instance.setType(template.getType());
        instance.setPrice(template.getBasePrice());
        instance.setStatus(SeatStatus.AVAILABLE);
        return instance;
    }

    /**
     * Mark showtimes as completed (background job).
     * Finds SCHEDULED showtimes with end_time in the past and marks them COMPLETED.
     * Should be called every 15 minutes by @Scheduled task.
     */
    @Transactional
    public void markCompletedShowtimes() {
        final Instant now = Instant.now();

        final List<Showtime> completedShowtimes = showtimeRepository.findScheduledShowtimesEndedBefore(now,
                ShowtimeStatus.SCHEDULED);

        if (completedShowtimes.isEmpty()) {
            return;
        }

        log.info("Marking {} showtimes as completed", completedShowtimes.size());

        completedShowtimes.forEach(showtime -> {
            showtime.setStatus(ShowtimeStatus.COMPLETED);
            log.debug("Showtime marked as completed: {}", showtime.getId());
        });

        showtimeRepository.saveAll(completedShowtimes);

        log.info("Marked {} showtimes as COMPLETED", completedShowtimes.size());
    }

    // ========== Validation Methods ==========

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

    private void validateNoScreenConflict(
            final Short screenNumber,
            final Instant startTime,
            final Instant endTime,
            final UUID excludeShowtimeId) {

        final boolean hasConflict = showtimeRepository.existsConflictingShowtime(
                screenNumber,
                startTime,
                endTime,
                excludeShowtimeId);

        if (hasConflict) {
            log.warn("Screen conflict detected for screen {} between {} and {}",
                    screenNumber, startTime, endTime);
            throw new ShowtimeConflictException(
                    "Screen " + screenNumber + " is already booked for this time slot");
        }
    }

    private void validateNoReservations(final UUID showtimeId) {
        final boolean hasReservations = reservationRepository.existsByShowtimeIdAndStatusIn(
                showtimeId,
                ReservationStatus.CONFIRMED,
                ReservationStatus.PENDING_PAYMENT);

        if (hasReservations) {
            throw new ShowtimeUpdateException("Cannot update showtime with existing reservations");
        }
    }

    private void validateCanBeCancelled(final Showtime showtime) {
        if (showtime.getStatus() == ShowtimeStatus.CANCELLED) {
            throw new ShowtimeAlreadyCancelledException("Showtime is already cancelled");
        }

        if (showtime.getStatus() == ShowtimeStatus.COMPLETED) {
            throw new ShowtimeCancellationException("Cannot cancel completed showtime");
        }
    }

    private void validateCanBeDeleted(final Showtime showtime) {
        // Cannot delete past showtimes
        if (showtime.getStartTime().isBefore(Instant.now())) {
            throw new ShowtimeDeletionException("Cannot delete past showtimes");
        }

        // Cannot delete if has reservations
        final boolean hasReservations = reservationRepository.existsByShowtimeIdAndStatusIn(
                showtime.getId(),
                ReservationStatus.CONFIRMED,
                ReservationStatus.PENDING_PAYMENT);

        if (hasReservations) {
            throw new ShowtimeDeletionException(
                    "Cannot delete showtime with existing reservations. Cancel the showtime instead.");
        }
    }
}