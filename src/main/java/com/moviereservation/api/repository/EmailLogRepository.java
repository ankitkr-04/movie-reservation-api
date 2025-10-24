package com.moviereservation.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moviereservation.api.domain.entities.EmailLog;
import com.moviereservation.api.domain.enums.EmailStatus;

/**
 * Repository for EmailLog entity operations.
 */
@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, UUID> {

    /**
     * Find failed emails eligible for retry.
     * Only includes emails with retry count less than max attempts.
     *
     * @param maxRetries Maximum number of retry attempts
     * @return List of failed emails to retry
     */
    @Query("""
            SELECT e FROM EmailLog e WHERE e.status = :failed \
            AND e.retryCount < :maxRetries \
            ORDER BY e.createdAt ASC""")
    List<EmailLog> findFailedEmailsForRetry(
            @Param("maxRetries") int maxRetries,
            @Param("failed") EmailStatus failed);

    /**
     * Find all email logs for a specific reservation.
     */
    List<EmailLog> findByReservationIdOrderByCreatedAtDesc(UUID reservationId);

    /**
     * Find all email logs for a specific user.
     */
    List<EmailLog> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Count emails by status.
     */
    long countByStatus(EmailStatus status);
}