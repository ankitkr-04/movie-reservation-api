package com.moviereservation.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moviereservation.api.domain.entities.Payment;
import com.moviereservation.api.domain.enums.PaymentStatus;

/**
 * Repository for Payment entity operations.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Find payment by Stripe PaymentIntent ID.
     * Used by webhook to identify which payment to update.
     */
    Optional<Payment> findByPaymentIntentId(String paymentIntentId);

    /**
     * Find payment by reservation ID.
     */
    @Query("SELECT p FROM Payment p WHERE p.reservation.id = :reservationId")
    Optional<Payment> findByReservationId(@Param("reservationId") UUID reservationId);

    /**
     * Find payment by reservation ID and status.
     * Used to find successful payment for refund processing.
     */
    @Query("SELECT p FROM Payment p WHERE p.reservation.id = :reservationId AND p.status = :status")
    Optional<Payment> findByReservationIdAndStatus(
            @Param("reservationId") UUID reservationId,
            @Param("status") PaymentStatus status);

    /**
     * Check if payment exists for reservation with any of the given statuses.
     * Used to prevent duplicate payment attempts.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Payment p " +
            "WHERE p.reservation.id = :reservationId AND p.status IN :statuses")
    boolean existsByReservationIdAndStatusIn(
            @Param("reservationId") UUID reservationId,
            @Param("statuses") PaymentStatus... statuses);
}