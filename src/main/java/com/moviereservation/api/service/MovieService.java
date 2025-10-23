package com.moviereservation.api.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviereservation.api.domain.entities.Movie;
import com.moviereservation.api.domain.entities.User;
import com.moviereservation.api.domain.enums.MovieStatus;
import com.moviereservation.api.domain.enums.ReservationStatus;
import com.moviereservation.api.exception.MovieAlreadyExistsException;
import com.moviereservation.api.exception.MovieDeletionException;
import com.moviereservation.api.exception.MovieNotFoundException;
import com.moviereservation.api.repository.MovieRepository;
import com.moviereservation.api.repository.ReservationRepository;
import com.moviereservation.api.repository.ShowtimeRepository;
import com.moviereservation.api.repository.specification.MovieSpecification;
import com.moviereservation.api.web.dto.request.movie.CreateMovieRequest;
import com.moviereservation.api.web.dto.request.movie.MovieFilterRequest;
import com.moviereservation.api.web.dto.request.movie.UpdateMovieRequest;
import com.moviereservation.api.web.mapper.MovieMapper;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for movie management.
 * Handles movie CRUD operations with proper validation and business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ReservationRepository reservationRepository;
    private final UserService userService;
    private final MovieMapper movieMapper;

    /**
     * Create a new movie.
     *
     * @param request Movie creation details
     * @return Created movie entity
     * @throws MovieAlreadyExistsException if movie with title already exists
     */
    @Transactional
    public Movie create(final CreateMovieRequest request) {
        log.debug("Creating movie: {}", request.getTitle());

        // Validate title uniqueness
        validateTitleAvailable(request.getTitle());

        // Map and save
        final Movie movie = movieMapper.toEntity(request);
        final Movie savedMovie = movieRepository.save(movie);

        log.info("Movie created: {} with ID: {}", savedMovie.getTitle(), savedMovie.getId());
        return savedMovie;
    }

    /**
     * Update an existing movie (partial update).
     *
     * @param movieId Movie ID
     * @param request Update details (only non-null fields updated)
     * @return Updated movie entity
     * @throws MovieNotFoundException if movie not found
     */
    @Transactional
    public Movie update(final UUID movieId, final UpdateMovieRequest request) {
        log.debug("Updating movie: {}", movieId);

        final Movie movie = findById(movieId);

        // Apply updates
        movieMapper.updateEntity(request, movie);
        final Movie updatedMovie = movieRepository.save(movie);

        log.info("Movie updated: {}", updatedMovie.getId());
        return updatedMovie;
    }

    /**
     * Update only movie status.
     * Convenience method for quick status transitions.
     *
     * @param movieId Movie ID
     * @param status  New status (ACTIVE, INACTIVE, COMING_SOON)
     * @return Updated movie entity
     */
    @Transactional
    public Movie updateStatus(final UUID movieId, final MovieStatus status) {
        log.debug("Updating movie status: {} to {}", movieId, status);

        final Movie movie = findById(movieId);
        movie.setStatus(status);
        final Movie updatedMovie = movieRepository.save(movie);

        log.info("Movie status updated: {} -> {}", movie.getId(), status);
        return updatedMovie;
    }

    /**
     * Soft delete a movie.
     * Cannot delete if movie has:
     * - Future showtimes
     * - Active reservations (CONFIRMED or PENDING_PAYMENT)
     *
     * @param movieId Movie ID
     * @param adminId Admin user performing deletion
     * @throws MovieNotFoundException if movie not found
     * @throws MovieDeletionException if deletion constraints violated
     */
    @Transactional
    public void delete(final UUID movieId, final UUID adminId) {
        log.debug("Deleting movie: {} by admin: {}", movieId, adminId);

        final Movie movie = findById(movieId);

        // Validate no future showtimes
        validateNoFutureShowtimes(movieId);

        // Validate no active reservations
        validateNoActiveReservations(movieId);

        // Set deleted by admin
        final User admin = userService.findById(adminId);
        movie.setDeletedBy(admin);

        // Soft delete
        movieRepository.delete(movie);

        log.info("Movie soft deleted: {}", movieId);
    }

    /**
     * Find movie by ID (admin access - all statuses).
     *
     * @throws MovieNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public Movie findById(final @NonNull UUID movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(movieId.toString()));
    }

    /**
     * Find movie by ID (customer access - visible statuses only).
     * Only returns ACTIVE or COMING_SOON movies.
     *
     * @throws MovieNotFoundException if not found or not customer-visible
     */
    @Transactional(readOnly = true)
    public Movie findByIdForCustomer(final UUID movieId) {
        return movieRepository.findOne(
                MovieSpecification.isNotDeleted()
                        .and(MovieSpecification.isVisibleToCustomers())
                        .and((root, _, cb) -> cb.equal(root.get("id"), movieId)))
                .orElseThrow(() -> new MovieNotFoundException(movieId.toString()));
    }

    /**
     * Find all movies for admin with filters and pagination.
     * Includes all statuses.
     */
    @Transactional(readOnly = true)
    public Page<Movie> findAllForAdmin(final Pageable pageable, final MovieFilterRequest filters) {
        log.debug("Finding movies for admin with filters: {}", filters);

        return movieRepository.findAll(
                MovieSpecification.forAdmin(filters),
                pageable);
    }

    /**
     * Find all movies for customer with filters and pagination.
     * Auto-restricts to ACTIVE and COMING_SOON statuses.
     */
    @Transactional(readOnly = true)
    public Page<Movie> findAllForCustomer(final Pageable pageable, final MovieFilterRequest filters) {
        log.debug("Finding movies for customer with filters: {}", filters);

        return movieRepository.findAll(
                MovieSpecification.forCustomer(filters),
                pageable);
    }

    // ========== Private Validation Methods ==========

    private void validateTitleAvailable(final String title) {
        if (movieRepository.existsByTitle(title)) {
            log.warn("Movie creation failed: Title already exists: {}", title);
            throw new MovieAlreadyExistsException(title);
        }
    }

    private void validateNoFutureShowtimes(final UUID movieId) {
        final boolean hasFutureShowtimes = showtimeRepository.existsByMovieIdAndStartTimeAfter(
                movieId,
                Instant.now());

        if (hasFutureShowtimes) {
            log.warn("Movie deletion failed: Has future showtimes: {}", movieId);
            throw new MovieDeletionException("Cannot delete movie with future showtimes");
        }
    }

    private void validateNoActiveReservations(final UUID movieId) {
        final boolean hasActiveReservations = reservationRepository.existsByShowtimeMovieIdAndStatusIn(
                movieId,
                ReservationStatus.CONFIRMED,
                ReservationStatus.PENDING_PAYMENT);

        if (hasActiveReservations) {
            log.warn("Movie deletion failed: Has active reservations: {}", movieId);
            throw new MovieDeletionException("Cannot delete movie with active reservations");
        }
    }
}