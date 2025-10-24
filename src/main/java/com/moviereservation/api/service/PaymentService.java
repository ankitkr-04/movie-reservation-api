package com.moviereservation.api.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviereservation.api.config.StripeConfig;
import com.moviereservation.api.constant.PaymentConstants;
import com.moviereservation.api.domain.entities.Payment;
import com.moviereservation.api.domain.entities.Reservation;
import com.moviereservation.api.domain.enums.PaymentStatus;
import com.moviereservation.api.domain.enums.ReservationStatus;
import com.moviereservation.api.exception.PaymentException;
import com.moviereservation.api.exception.PaymentNotFoundException;
import com.moviereservation.api.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for payment processing via Stripe.
 * Handles payment intent creation, webhook processing, and refunds.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationService reservationService;
    private final StripeConfig stripeConfig;

    /**
     * Create a Stripe PaymentIntent for a reservation.
     * Returns client secret for frontend to complete payment.
     *
     * @param reservationId Reservation ID
     * @param userId        User ID (for verification)
     * @return Client secret for Stripe.js
     * @throws PaymentException if payment intent creation fails
     */
    @Transactional
    public String createPaymentIntent(UUID reservationId, UUID userId) {
        log.debug("Creating payment intent for reservation: {}", reservationId);

        // Fetch and validate reservation
        Reservation reservation = reservationService.findById(reservationId);
        validateReservationForPayment(reservation, userId);

        // Convert amount to paise (Stripe uses smallest currency unit)
        long amountInCents = reservation.getTotalPrice()
                .multiply(new BigDecimal("100"))
                .longValue();

        try {
            // Create Stripe PaymentIntent
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(stripeConfig.getCurrency().toLowerCase())
                    .setDescription(String.format(
                            PaymentConstants.PAYMENT_DESCRIPTION_FORMAT,
                            reservation.getBookingReference()))
                    .putMetadata("reservationId", reservationId.toString())
                    .putMetadata("bookingReference", reservation.getBookingReference())
                    .putMetadata("userId", userId.toString())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Create payment record
            createPaymentRecord(reservation, paymentIntent);

            log.info("Payment intent created: {} for reservation: {}",
                    paymentIntent.getId(), reservationId);

            return paymentIntent.getClientSecret();

        } catch (StripeException e) {
            log.error("Failed to create payment intent for reservation: {}", reservationId, e);
            throw new PaymentException("Failed to create payment intent: " + e.getMessage());
        }
    }

    /**
     * Process Stripe webhook event.
     * Handles payment success, failure, and cancellation.
     *
     * @param paymentIntentId Stripe PaymentIntent ID
     * @param eventType       Stripe event type
     * @throws PaymentNotFoundException if payment not found
     */
    @Transactional
    public void processWebhookEvent(String paymentIntentId, String eventType) {
        log.debug("Processing webhook event: {} for PaymentIntent: {}", eventType, paymentIntentId);

        Payment payment = paymentRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for PaymentIntent: " + paymentIntentId));

        switch (eventType) {
            case PaymentConstants.EVENT_PAYMENT_INTENT_SUCCEEDED:
                handlePaymentSuccess(payment);
                break;

            case PaymentConstants.EVENT_PAYMENT_INTENT_FAILED:
                handlePaymentFailure(payment);
                break;

            case PaymentConstants.EVENT_PAYMENT_INTENT_CANCELED:
                handlePaymentCancellation(payment);
                break;

            default:
                log.warn("Unhandled webhook event type: {}", eventType);
        }
    }

    /**
     * Process refund for a reservation.
     * Creates Stripe refund and records it in database.
     *
     * @param reservationId Reservation ID
     * @param reason        Refund reason
     * @throws PaymentException if refund fails
     */
    @Transactional
    public void processRefund(UUID reservationId, String reason) {
        log.debug("Processing refund for reservation: {}", reservationId);

        Reservation reservation = reservationService.findById(reservationId);

        // Find successful payment for this reservation
        Payment payment = paymentRepository.findByReservationIdAndStatus(
                reservationId, PaymentStatus.PAID)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "No successful payment found for reservation: " + reservationId));

        try {
            // Create Stripe refund
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(payment.getPaymentIntentId())
                    .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                    .putMetadata("reservationId", reservationId.toString())
                    .putMetadata("reason", reason)
                    .build();

            Refund refund = Refund.create(params);

            // Update payment status
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            // Update reservation status
            reservation.setStatus(ReservationStatus.REFUNDED);
            // Note: Reservation is saved by ReservationService

            log.info("Refund processed: {} for reservation: {}", refund.getId(), reservationId);

        } catch (StripeException e) {
            log.error("Failed to process refund for reservation: {}", reservationId, e);
            throw new PaymentException("Failed to process refund: " + e.getMessage());
        }
    }

    /**
     * Find payment by reservation ID.
     *
     * @param reservationId Reservation ID
     * @return Payment entity
     * @throws PaymentNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public Payment findByReservationId(UUID reservationId) {
        return paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for reservation: " + reservationId));
    }

    // ========== Private Helper Methods ==========

    /**
     * Create payment record in database.
     */
    private void createPaymentRecord(Reservation reservation, PaymentIntent paymentIntent) {
        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setUser(reservation.getUser());
        payment.setAmount(reservation.getTotalPrice());
        payment.setCurrency(stripeConfig.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(PaymentConstants.PAYMENT_METHOD_STRIPE);
        payment.setPaymentIntentId(paymentIntent.getId());
        payment.setAttemptNumber((short) 1);

        paymentRepository.save(payment);

        log.debug("Payment record created for reservation: {}", reservation.getId());
    }

    /**
     * Handle successful payment.
     */
    private void handlePaymentSuccess(Payment payment) {
        log.info("Processing payment success for PaymentIntent: {}", payment.getPaymentIntentId());

        // Update payment status
        payment.setStatus(PaymentStatus.PAID);

        try {
            // Retrieve the PaymentIntent from Stripe
            PaymentIntent paymentIntent = PaymentIntent.retrieve(payment.getPaymentIntentId());

            // Safely extract the latest charge ID (works in Stripe Java SDK v24+)
            String chargeId = paymentIntent.getLatestCharge();
            if (chargeId != null) {
                payment.setChargeId(chargeId);
            } else {
                log.warn("No charge found for PaymentIntent: {}", payment.getPaymentIntentId());
            }

        } catch (StripeException e) {
            log.warn("Failed to fetch charge ID for PaymentIntent: {}", payment.getPaymentIntentId(), e);
        }

        // Persist payment update
        paymentRepository.save(payment);

        // Update reservation status
        Reservation reservation = payment.getReservation();
        reservation.setStatus(ReservationStatus.CONFIRMED);

        // If not cascaded, explicitly persist reservation
        // reservationRepository.save(reservation);

        // TODO: Send booking confirmation email asynchronously
        // emailService.sendBookingConfirmation(reservation);

        log.info("Payment successful for reservation: {}", reservation.getBookingReference());
    }

    /**
     * Handle failed payment.
     */
    private void handlePaymentFailure(Payment payment) {
        log.warn("Processing payment failure for PaymentIntent: {}", payment.getPaymentIntentId());

        // Update payment status
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        // Reservation remains in PENDING_PAYMENT
        // Will be expired by scheduled task if not paid within 5 minutes

        log.info("Payment failed for reservation: {}", payment.getReservation().getBookingReference());
    }

    /**
     * Handle cancelled payment.
     */
    private void handlePaymentCancellation(Payment payment) {
        log.info("Processing payment cancellation for PaymentIntent: {}", payment.getPaymentIntentId());

        // Update payment status
        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);

        // Reservation remains in PENDING_PAYMENT
        // Will be expired by scheduled task

        log.info("Payment cancelled for reservation: {}", payment.getReservation().getBookingReference());
    }

    // ========== Validation Methods ==========

    /**
     * Validate reservation can be paid.
     */
    private void validateReservationForPayment(Reservation reservation, UUID userId) {
        // Verify ownership
        if (!reservation.getUser().getId().equals(userId)) {
            throw new PaymentException("You do not have permission to pay for this reservation");
        }

        // Must be in PENDING_PAYMENT status
        if (reservation.getStatus() != ReservationStatus.PENDING_PAYMENT) {
            throw new PaymentException(
                    "Reservation cannot be paid. Current status: " + reservation.getStatus());
        }

        // Check if payment already exists
        boolean paymentExists = paymentRepository.existsByReservationIdAndStatusIn(
                reservation.getId(),
                PaymentStatus.PAID,
                PaymentStatus.PENDING);

        if (paymentExists) {
            throw new PaymentException("Payment already initiated or completed for this reservation");
        }
    }
}