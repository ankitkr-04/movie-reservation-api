package com.moviereservation.api.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moviereservation.api.domain.entities.Showtime;
import com.moviereservation.api.domain.enums.ShowtimeStatus;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, UUID>, JpaSpecificationExecutor<Showtime> {
  /**
   * Check if a movie has any showtimes scheduled after a specific time.
   * Used to prevent deletion of movies with future showtimes.
   * 
   * @param movieId
   * @param startTime
   * @return true if such showtimes exist
   */
  boolean existsByMovieIdAndStartTimeAfter(UUID movieId, Instant startTime);

  /**
   * Check for conflicting showtimes on the same screen.
   * Used to prevent scheduling overlapping showtimes.
   * 
   * @param screenNumber
   * @param startTime
   * @param endTime
   * @param excludeShowtimeId
   * @return true if conflicting showtimes exist
   */
  @Query("""
        SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
        FROM Showtime s
        WHERE s.screenNumber = :screenNumber
          AND s.status <> 'CANCELLED'
          AND (:excludeId IS NULL OR s.id <> :excludeId)
          AND s.deletedAt IS NULL
          AND (s.startTime < :endTime AND s.endTime > :startTime)
      """)
  boolean existsConflictingShowtime(
      @Param("screenNumber") Short screenNumber,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime,
      @Param("excludeShowtimeId") UUID excludeShowtimeId);

  @Query("""
        SELECT s FROM Showtime s
        WHERE s.endTime < :now
          AND s.status = :scheduled
          AND s.deletedAt IS NULL
      """)
      List<Showtime> findScheduledShowtimesEndedBefore(@Param("now") Instant now,
      @Param("scheduled") ShowtimeStatus scheduled);

}