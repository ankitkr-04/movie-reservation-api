package com.moviereservation.api.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moviereservation.api.domain.entities.Movie;
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

import lombok.RequiredArgsConstructor;

/**
 * Service layer for Movie operations.
 * Handles business logic and delegates to repository.
 */
@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ReservationRepository reservationRepository;
    private final MovieMapper movieMapper;

    /**
     * Create a new movie.
     * 
     * @throws MovieAlreadyExistsException if title already exists
     */
    public Movie create(final CreateMovieRequest movieRequest) {
        if (movieRepository.existsByTitle(movieRequest.getTitle())) {
            throw new MovieAlreadyExistsException(movieRequest.getTitle());
        }

        final Movie movie = new Movie();
        movieMapper.toEntity(movieRequest, movie);

        return movieRepository.save(movie);
    }

    /**
     * Update an existing movie.
     * 
     * @throws MovieNotFoundException if movie not found
     */
    public Movie update(final UUID movieId, final UpdateMovieRequest request) {
        final Movie movie = findById(movieId);
        movieMapper.toEntity(request, movie);
        return movieRepository.save(movie);
    }

    /**
     * Find movie by ID (admin access - includes all statuses).
     * 
     * @throws MovieNotFoundException if not found
     */
    public Movie findById(final UUID movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(movieId.toString()));
    }

    /**
     * Delete movie by ID.
     * Cannot delete if movie has future showtimes or active reservations.
     * 
     * @throws MovieNotFoundException if movie not found
     * @throws MovieDeletionException if movie has future showtimes or active
     *                                reservations
     */

    public void deleteById(final UUID movieId) {
        final Movie movie = findById(movieId);
        final boolean hasFutureShowtimes = showtimeRepository.existsByMovieIdAndStartTimeAfter(
                movieId,
                Instant.now());

        if (hasFutureShowtimes) {
            throw new MovieDeletionException(movieId.toString());
        }

        final boolean hasActiveReservations = reservationRepository.existsByShowtimeMovieIdAndStatusIn(
                movieId,
                ReservationStatus.CONFIRMED,
                ReservationStatus.PENDING_PAYMENT);

        if (hasActiveReservations) {
            throw new MovieDeletionException(movieId.toString());
        }
        movieRepository.delete(movie);
    }

    /**
     * Find all movies for admin with filters and pagination.
     * Includes all movie statuses.
     */
    public Page<Movie> findAllForAdmin(final Pageable pageable, final MovieFilterRequest filters) {
        return movieRepository.findAll(
                MovieSpecification.forAdmin(filters),
                pageable);
    }

    /**
     * Find all movies for customers with filters and pagination.
     * Automatically restricts to customer-visible statuses.
     */
    public Page<Movie> findAllForCustomer(final Pageable pageable, final MovieFilterRequest filters) {
        return movieRepository.findAll(
                MovieSpecification.forCustomer(filters),
                pageable);
    }

    /**
     * Find movie by ID for customer (only visible statuses).
     * 
     * @throws MovieNotFoundException if not found or not visible to customers
     */
    public Movie findByIdForCustomer(final UUID movieId) {
        return movieRepository.findOne(
                MovieSpecification.isNotDeleted()
                        .and(MovieSpecification.isVisibleToCustomers())
                        .and((root, _, cb) -> cb.equal(root.get("id"), movieId)))
                .orElseThrow(() -> new MovieNotFoundException(movieId.toString()));
    }
}
