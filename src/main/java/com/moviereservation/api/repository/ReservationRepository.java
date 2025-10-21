package com.moviereservation.api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moviereservation.api.domain.entities.Reservation;
import com.moviereservation.api.domain.enums.ReservationStatus;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID>, JpaSpecificationExecutor<Reservation> {

    /**
     * Check if a movie has any reservations with specific statuses.
     * Used to prevent deletion of movies with active reservations.
     * 
     * @param movie    the movie to check
     * @param statuses variable number of reservation statuses to check
     * @return true if reservations with given statuses exist
     */
    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END\
            FROM Reservation r \
            WHERE r.showtime.movie.id = :movieId \
            AND r.status IN :statuses""")
    boolean existsByShowtimeMovieIdAndStatusIn(@Param("movieId") UUID movieId,
            @Param("statuses") ReservationStatus... statuses);

    /**
     * Check if a showtime has any reservations with specific statuses.
     * Used to check for active reservations for a showtime.
     * 
     * @param showtimeId
     * @param statuses
     * @return true if reservations with given statuses exist
     */
    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END\
            FROM Reservation r \
            WHERE r.showtime.id = :showtimeId \
            AND r.status IN :statuses""")
    boolean existsByShowtimeIdAndStatusIn(UUID showtimeId, ReservationStatus... statuses);

}
