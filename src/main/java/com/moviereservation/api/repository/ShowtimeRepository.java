package com.moviereservation.api.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.moviereservation.api.domain.entities.Showtime;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, UUID>, JpaSpecificationExecutor<Showtime> {
    boolean existsByMovieIdAndStartTimeAfter(UUID movieId, Instant startTime);

}