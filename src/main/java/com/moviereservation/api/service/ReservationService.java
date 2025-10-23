package com.moviereservation.api.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviereservation.api.domain.entities.*;
import com.moviereservation.api.domain.enums.ReservationStatus;
import com.moviereservation.api.domain.enums.SeatStatus;
import com.moviereservation.api.domain.enums.ShowtimeStatus;
import com.moviereservation.api.exception.*;
import com.moviereservation.api.repository.ReservationRepository;
import com.moviereservation.api.repository.SeatInstanceRepository;
import com.moviereservation.api.repository.ShowtimeRepository;
import com.moviereservation.api.repository.specification.ReservationSpecification;
import com.moviereservation.api.web.dto.request.reservation.CreateReservationRequest;
import com.moviereservation.api.web.dto.request.reservation.ReservationFilterRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for reservation management.
 * Handles booking, cancellation, and seat hold management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    // Constants
    private static final int SEAT_HOLD_MINUTES = 5;
    private static final int MIN_CANCEL_HOURS = 2;
    private static final int MAX_SEATS_PER_BOOKING = 10;

    private final ReservationRepository reservationRepository;
    private final SeatInstanceRepository seatInstanceRepository;
    private final ShowtimeRepository showtimeRepository;
    private final UserService userService;

    /**
     * Create a new reservation (book seats).
     * Seats are held with pessimistic locking to prevent double-booking.
     * Status is set to PENDING_PAYMENT with 5-minute hold timer.
     *
     * @param userId  User ID making the reservation
     * @param request Reservation details (showtime and seat IDs)
     * @return Created reservation entity
     * @throws InvalidReservationException if validation fails
     * @throws SeatUnavailableException    if seats not available
     */
    @Transactional
    public Reservation create(final UUID userId, final CreateReservationRequest request) {
        log.debug("Creating reservation for user: {} with {} seats",
                userId, request.getSeatInstanceIds().size());

        // Validate request
        validateSeatCount(request.getSeatInstanceIds().size());

        // Fetch entities
        final User user = userService.findById(userId);
        final Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new ShowtimeNotFoundException("Showtime not found"));

        // Validate showtime is bookable
        validateShowtimeBookable(showtime);

        // Lock and validate seats (pessimistic locking)
        final List<SeatInstance> seats = lockAndValidateSeats(
                request.getSeatInstanceIds(),
                showtime.getId(),
                userId);

        // Hold seats
        final Instant now = Instant.now();
        holdSeats(seats, user, now);

        // Calculate total price
        final BigDecimal totalPrice = calculateTotalPrice(seats);

        // Create reservation
        final Reservation reservation = buildReservation(user, showtime, totalPrice, seats);

        final Reservation savedReservation = reservationRepository.save(reservation);

        log.info("Reservation created: {} for user: {} with {} seats (Total: {})",
                savedReservation.getBookingReference(), userId, seats.size(), totalPrice);

        return savedReservation;
    }

    /**
     * Cancel a reservation by customer.
     * Must be at least 2 hours before showtime.
     * Can only cancel CONFIRMED reservations.
     *
     * @param bookingReference Booking reference
     * @param userId           User ID (for authorization)
     * @return Cancelled reservation entity
     * @throws InvalidReservationCancellationException if cannot be cancelled
     */
    @Transactional
    public Reservation cancel(final String bookingReference, final UUID userId) {
        log.debug("Cancelling reservation: {} by user: {}", bookingReference, userId);

        final Reservation reservation = findByBookingReferenceAndUser(bookingReference, userId);

        // Validate can be cancelled
        validateCanBeCancelled(reservation);

        // Mark as cancelled
        reservation.setStatus(ReservationStatus.CANCELLED);
        final Reservation cancelledReservation = reservationRepository.save(reservation);

        // Release seats
        releaseSeats(reservation);

        // Update showtime available count
        updateShowtimeAvailableSeats(reservation.getShowtime(), reservation.getReservationSeats().size());

        log.info("Reservation cancelled: {} by user: {}", bookingReference, userId);

        return cancelledReservation;
    }

    /**
     * Cancel a reservation by admin.
     * Bypasses the 2-hour cancellation policy.
     *
     * @param reservationId Reservation ID
     * @return Cancelled reservation entity
     */
    @Transactional
    public Reservation cancelByAdmin(final UUID reservationId) {
        log.debug("Admin cancelling reservation: {}", reservationId);

        final Reservation reservation = findById(reservationId);

        // Admin can cancel even within 2 hours
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new InvalidReservationCancellationException(
                    "Can only cancel CONFIRMED reservations. Current status: " + reservation.getStatus());
        }

        // Mark as cancelled
        reservation.setStatus(ReservationStatus.CANCELLED);
        final Reservation cancelledReservation = reservationRepository.save(reservation);

        // Release seats
        releaseSeats(reservation);

        // Update showtime available count
        updateShowtimeAvailableSeats(reservation.getShowtime(), reservation.getReservationSeats().size());

        log.info("Reservation cancelled by admin: {}", reservationId);

        return cancelledReservation;
    }

    /**
     * Find reservation by ID (all access).
     *
     * @throws ReservationNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public Reservation findById(final UUID reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId.toString()));
    }

    /**
     * Find reservation by booking reference and user (customer access).
     * Verifies ownership.
     *
     * @throws ReservationNotFoundException           if not found
     * @throws UnauthorizedReservationAccessException if not owned by user
     */
    @Transactional(readOnly = true)
    public Reservation findByBookingReferenceAndUser(final String bookingReference, final UUID userId) {
        final Reservation reservation = reservationRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + bookingReference));

        // Verify ownership
        if (!reservation.getUser().getId().equals(userId)) {
            log.warn("Unauthorized access attempt to reservation: {} by user: {}", bookingReference, userId);
            throw new UnauthorizedReservationAccessException(
                    "You do not have permission to access this reservation");
        }

        return reservation;
    }

    /**
     * Find reservation by booking reference (admin access).
     *
     * @throws ReservationNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public Reservation findByBookingReference(final String bookingReference) {
        return reservationRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + bookingReference));
    }

    /**
     * Find all reservations for a customer with filters and pagination.
     * Auto-scoped to user's own reservations.
     */
    @Transactional(readOnly = true)
    public Page<Reservation> findAllForCustomer(
            final UUID userId,
            final Pageable pageable,
            final ReservationFilterRequest filters) {

        log.debug("Finding reservations for customer: {}", userId);

        return reservationRepository.findAll(
                ReservationSpecification.forCustomer(userId, filters),
                pageable);
    }

    /**
     * Find all reservations for admin with filters and pagination.
     * Includes all users' reservations.
     */
    @Transactional(readOnly = true)
    public Page<Reservation> findAllForAdmin(
            final Pageable pageable,
            final ReservationFilterRequest filters) {

        log.debug("Finding reservations for admin");

        return reservationRepository.findAll(
                ReservationSpecification.forAdmin(filters),
                pageable);
    }

    /**
     * Process expired holds (background job).
     * Releases seats and marks reservations as EXPIRED.
     * Should be called every minute by @Scheduled task.
     */
    @Transactional
    public void processExpiredHolds() {
        final Instant expiryTime = Instant.now().minus(SEAT_HOLD_MINUTES, ChronoUnit.MINUTES);

        final List<Reservation> expiredReservations = reservationRepository
                .findExpiredPendingReservations(expiryTime, ReservationStatus.PENDING_PAYMENT);

        if (expiredReservations.isEmpty()) {
            return;
        }

        log.info("Processing {} expired holds", expiredReservations.size());

        for (final Reservation reservation : expiredReservations) {
            expireReservation(reservation);
        }

        reservationRepository.saveAll(expiredReservations);

        log.info("Processed {} expired reservations", expiredReservations.size());
    }

    // ========== Private Helper Methods ==========

    /**
     * Lock seats with SELECT FOR UPDATE and validate availability.
     */
    private List<SeatInstance> lockAndValidateSeats(
            final List<UUID> seatIds,
            final UUID showtimeId,
            final UUID userId) {

        // Lock seats (pessimistic locking)
        final List<SeatInstance> seats = seatInstanceRepository.findAllByIdWithLock(seatIds);

        // Validate all seats found
        if (seats.size() != seatIds.size()) {
            throw new SeatNotFoundException("One or more seats not found");
        }

        // Validate all seats belong to this showtime
        final boolean allBelongToShowtime = seats.stream()
                .allMatch(seat -> seat.getShowtime().getId().equals(showtimeId));

        if (!allBelongToShowtime) {
            throw new InvalidReservationException("All seats must belong to the same showtime");
        }

        // Check availability and release expired holds
        final Instant now = Instant.now();
        for (final SeatInstance seat : seats) {
            validateSeatAvailability(seat, userId, now);
        }

        return seats;
    }

    /**
     * Validate single seat availability.
     */
    private void validateSeatAvailability(final SeatInstance seat, final UUID userId, final Instant now) {
        if (seat.getStatus() == SeatStatus.RESERVED) {
            throw new SeatUnavailableException(
                    "Seat " + seat.getRowLabel() + seat.getSeatNumber() + " is already booked");
        }

        if (seat.getStatus() == SeatStatus.HELD) {
            // Check if hold expired
            if (isHoldExpired(seat, now)) {
                // Release expired hold
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHeldAt(null);
                seat.setHeldBy(null);
            } else if (!seat.getHeldBy().getId().equals(userId)) {
                // Different user holding the seat
                throw new SeatUnavailableException(
                        "Seat " + seat.getRowLabel() + seat.getSeatNumber() +
                                " is currently held by another user");
            }
        }
    }

    /**
     * Hold seats for a user.
     */
    private void holdSeats(final List<SeatInstance> seats, final User user, final Instant now) {
        seats.forEach(seat -> {
            seat.setStatus(SeatStatus.HELD);
            seat.setHeldAt(now);
            seat.setHeldBy(user);
        });
        seatInstanceRepository.saveAll(seats);
    }

    /**
     * Release seats back to AVAILABLE.
     */
    private void releaseSeats(final Reservation reservation) {
        final List<UUID> seatIds = reservation.getReservationSeats().stream()
                .map(rs -> rs.getSeatInstance().getId())
                .toList();

        final List<SeatInstance> seats = seatInstanceRepository.findAllById(seatIds);
        seats.forEach(seat -> {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setHeldAt(null);
            seat.setHeldBy(null);
        });
        seatInstanceRepository.saveAll(seats);

        log.debug("Released {} seats for reservation: {}",
                seats.size(), reservation.getBookingReference());
    }

    /**
     * Calculate total price from seat list.
     */
    private BigDecimal calculateTotalPrice(final List<SeatInstance> seats) {
        return seats.stream()
                .map(SeatInstance::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Build reservation entity with seats.
     */
    private Reservation buildReservation(
            final User user,
            final Showtime showtime,
            final BigDecimal totalPrice,
            final List<SeatInstance> seats) {

        final Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setShowtime(showtime);
        reservation.setStatus(ReservationStatus.PENDING_PAYMENT);
        reservation.setTotalPrice(totalPrice);
        reservation.setBookingReference(generateBookingReference());

        // Add seats
        seats.forEach(seat -> {
            final ReservationSeat reservationSeat = new ReservationSeat();
            reservationSeat.setSeatInstance(seat);
            reservationSeat.setPricePaid(seat.getPrice());
            reservation.addSeat(reservationSeat);
        });

        return reservation;
    }

    /**
     * Expire a reservation and release its seats.
     */
    private void expireReservation(final Reservation reservation) {
        reservation.setStatus(ReservationStatus.EXPIRED);

        releaseSeats(reservation);
        updateShowtimeAvailableSeats(reservation.getShowtime(), reservation.getReservationSeats().size());

        log.info("Reservation expired: {}", reservation.getBookingReference());
    }

    /**
     * Update showtime available seats count.
     */
    private void updateShowtimeAvailableSeats(final Showtime showtime, final int seatCount) {
        showtime.setAvailableSeatsCount(
                (short) (showtime.getAvailableSeatsCount() + seatCount));
        showtimeRepository.save(showtime);
    }

    /**
     * Generate unique booking reference.
     */
    private String generateBookingReference() {
        return "BK" + UUID.randomUUID().toString()
                .substring(0, 6)
                .toUpperCase();
    }

    /**
     * Check if seat hold has expired.
     */
    private boolean isHoldExpired(final SeatInstance seat, final Instant now) {
        return seat.getHeldAt() != null &&
                seat.getHeldAt().plus(SEAT_HOLD_MINUTES, ChronoUnit.MINUTES).isBefore(now);
    }

    // ========== Validation Methods ==========

    private void validateSeatCount(final int count) {
        if (count == 0) {
            throw new InvalidReservationException("At least one seat must be selected");
        }

        if (count > MAX_SEATS_PER_BOOKING) {
            throw new InvalidReservationException(
                    "Cannot reserve more than " + MAX_SEATS_PER_BOOKING + " seats per booking");
        }
    }

    private void validateShowtimeBookable(final Showtime showtime) {
        final Instant now = Instant.now();

        if (showtime.getStartTime().isBefore(now)) {
            throw new InvalidReservationException("Cannot book seats for past showtimes");
        }

        if (showtime.getStatus() == ShowtimeStatus.CANCELLED) {
            throw new InvalidReservationException("Cannot book seats for cancelled showtimes");
        }

        if (showtime.getStatus() == ShowtimeStatus.COMPLETED) {
            throw new InvalidReservationException("Cannot book seats for completed showtimes");
        }

        if (showtime.getAvailableSeatsCount() <= 0) {
            throw new InvalidReservationException("No available seats for the selected showtime");
        }
    }

    private void validateCanBeCancelled(final Reservation reservation) {
        // Can only cancel CONFIRMED reservations
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new InvalidReservationCancellationException(
                    "Only CONFIRMED reservations can be cancelled. Current status: " + reservation.getStatus());
        }

        final Showtime showtime = reservation.getShowtime();
        final Instant now = Instant.now();

        // Cannot cancel past showtimes
        if (showtime.getStartTime().isBefore(now)) {
            throw new InvalidReservationCancellationException("Cannot cancel reservation for past showtime");
        }

        // Must cancel at least 2 hours before showtime
        final Instant minCancelTime = showtime.getStartTime().minus(MIN_CANCEL_HOURS, ChronoUnit.HOURS);
        if (now.isAfter(minCancelTime)) {
            throw new InvalidReservationCancellationException(
                    "Reservations must be cancelled at least " + MIN_CANCEL_HOURS + " hours before showtime");
        }
    }
}