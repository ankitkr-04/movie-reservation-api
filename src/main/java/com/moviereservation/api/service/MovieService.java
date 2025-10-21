package com.moviereservation.api.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moviereservation.api.domain.entities.Movie;
import com.moviereservation.api.exception.business.movie.MovieAlreadyExistsException;
import com.moviereservation.api.exception.business.movie.MovieNotFoundException;
import com.moviereservation.api.repository.MovieRepository;
import com.moviereservation.api.repository.specification.MovieSpecification;
import com.moviereservation.api.web.dto.request.movie.CreateMovieRequest;
import com.moviereservation.api.web.dto.request.movie.FilterMovieRequest;
import com.moviereservation.api.web.dto.request.movie.UpdateMovieRequest;
import com.moviereservation.api.web.mapper.MovieMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;

    public Movie createMovie(final CreateMovieRequest movieRequest) {
        if (movieRepository.existsByTitle(movieRequest.getTitle())) {
            throw new MovieAlreadyExistsException(movieRequest.getTitle());
        }

        final Movie movie = new Movie();
        movieMapper.toEntity(movieRequest, movie);

        return movieRepository.save(movie);

    }

    public Movie updateMovie(final UUID movieId,
            final UpdateMovieRequest request) {
        final Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(movieId.toString()));

        movieMapper.toEntity(request, movie);

        return movieRepository.save(movie);
    }

    public Movie getMovieById(final UUID movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(movieId.toString()));
    }

    public Page<Movie> getAllMoviesForAdmin(final Pageable pageable, final FilterMovieRequest filters) {
        return movieRepository.findAll(
                MovieSpecification.forAdmin(filters),
                pageable);
    }

    public Page<Movie> getAllMoviesForCustomer(final Pageable pageable, final FilterMovieRequest filters) {
        return movieRepository.findAll(
                MovieSpecification.forCustomer(filters),
                pageable);
    }

    public Movie getMovieByIdForCustomer(final UUID movieId) {
        return movieRepository.findOne(
                MovieSpecification.isNotDeleted()
                        .and(MovieSpecification.isVisibleToCustomers())
                        .and((root, _, cb) -> cb.equal(root.get("id"), movieId)))
                .orElseThrow(() -> new MovieNotFoundException(movieId.toString()));
    }

}
