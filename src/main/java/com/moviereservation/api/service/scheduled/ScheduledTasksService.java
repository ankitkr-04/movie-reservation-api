package com.moviereservation.api.service.scheduled;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.moviereservation.api.service.ReservationService;
import com.moviereservation.api.service.ShowtimeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled background tasks service.
 * Handles periodic operations like:
 * - Releasing expired seat holds
 * - Marking completed showtimes
 * - Cleanup operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasksService {

    private final ReservationService reservationService;
    private final ShowtimeService showtimeService;

    /**
     * Release expired seat holds.
     * Runs every 1 minute.
     * 
     * - Finds reservations in PENDING_PAYMENT status older than 5 minutes
     * - Marks them as EXPIRED
     * - Releases held seats back to AVAILABLE
     * - Updates showtime available seats count
     */
    @Scheduled(fixedRate = 60_000) // Every 60 seconds
    public void releaseExpiredHolds() {
        log.debug("Running scheduled task: Release expired holds");

        try {
            reservationService.processExpiredHolds();
        } catch (final Exception e) {
            log.error("Error processing expired holds", e);
        }
    }

    /**
     * Mark completed showtimes.
     * Runs every 15 minutes.
     * 
     * - Finds showtimes with end_time in the past
     * - Status is still SCHEDULED
     * - Marks them as COMPLETED
     */
    @Scheduled(fixedRate = 900_000) // Every 15 minutes
    public void markCompletedShowtimes() {
        log.debug("Running scheduled task: Mark completed showtimes");

        try {
            showtimeService.markCompletedShowtimes();
        } catch (final Exception e) {
            log.error("Error marking completed showtimes", e);
        }
    }

    /**
     * Cleanup old data (optional).
     * Runs daily at 2 AM.
     * 
     * - Archive old reservations
     * - Cleanup expired tokens
     * - Database maintenance
     */
    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2 AM
    public void dailyCleanup() {
        log.info("Running scheduled task: Daily cleanup");

        try {
            // TODO: Implement cleanup operations
            // - Archive reservations older than 1 year
            // - Cleanup expired tokens
            // - Database optimization
        } catch (final Exception e) {
            log.error("Error during daily cleanup", e);
        }
    }
}