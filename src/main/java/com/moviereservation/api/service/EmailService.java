package com.moviereservation.api.service;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviereservation.api.domain.entities.EmailLog;
import com.moviereservation.api.domain.entities.Reservation;
import com.moviereservation.api.domain.entities.ReservationSeat;
import com.moviereservation.api.domain.enums.EmailStatus;
import com.moviereservation.api.domain.enums.EmailType;
import com.moviereservation.api.repository.EmailLogRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for sending email notifications.
 * All email operations are asynchronous to avoid blocking API responses.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;

    /**
     * Send booking confirmation email (on payment success).
     * Called asynchronously from payment webhook handler.
     */
    @Async
    @Transactional
    public void sendBookingConfirmation(final Reservation reservation) {
        log.debug("Sending booking confirmation for: {}", reservation.getBookingReference());

        final String subject = "Booking Confirmed - " + reservation.getBookingReference();
        final String body = buildBookingConfirmationEmail(reservation);

        sendEmailAsync(
                reservation.getUser().getEmail(),
                subject,
                body,
                EmailType.BOOKING_CONFIRMATION,
                reservation.getUser().getId(),
                reservation.getId());
    }

    /**
     * Send cancellation confirmation email (user-initiated).
     */
    @Async
    @Transactional
    public void sendCancellationConfirmation(final Reservation reservation) {
        log.debug("Sending cancellation confirmation for: {}", reservation.getBookingReference());

        final String subject = "Booking Cancelled - " + reservation.getBookingReference();
        final String body = buildCancellationEmail(reservation);

        sendEmailAsync(
                reservation.getUser().getEmail(),
                subject,
                body,
                EmailType.CANCELLATION_CONFIRMATION,
                reservation.getUser().getId(),
                reservation.getId());
    }

    /**
     * Send refund notification email (showtime cancelled by admin).
     */
    @Async
    @Transactional
    public void sendRefundNotification(final Reservation reservation, final String reason) {
        log.debug("Sending refund notification for: {}", reservation.getBookingReference());

        final String subject = "Refund Processed - " + reservation.getBookingReference();
        final String body = buildRefundEmail(reservation, reason);

        sendEmailAsync(
                reservation.getUser().getEmail(),
                subject,
                body,
                EmailType.REFUND_NOTIFICATION,
                reservation.getUser().getId(),
                reservation.getId());
    }

    /**
     * Send payment failure notification.
     */
    @Async
    @Transactional
    public void sendPaymentFailureNotification(final Reservation reservation, final String errorMessage) {
        log.debug("Sending payment failure notification for: {}", reservation.getBookingReference());

        final String subject = "Payment Failed - " + reservation.getBookingReference();
        final String body = buildPaymentFailureEmail(reservation, errorMessage);

        sendEmailAsync(
                reservation.getUser().getEmail(),
                subject,
                body,
                EmailType.PAYMENT_FAILURE,
                reservation.getUser().getId(),
                reservation.getId());
    }

    /**
     * Retry failed emails (called by scheduled task).
     */
    @Transactional
    public void retryFailedEmails() {
        final var failedEmails = emailLogRepository.findFailedEmailsForRetry(MAX_RETRY_ATTEMPTS, EmailStatus.FAILED);

        if (failedEmails.isEmpty()) {
            return;
        }

        log.info("Retrying {} failed emails", failedEmails.size());

        for (final EmailLog emailLog : failedEmails) {
            retryEmail(emailLog);
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * Send email and log the attempt.
     */
    private void sendEmailAsync(
            final String recipientEmail,
            final String subject,
            final String body,
            final EmailType emailType,
            final UUID userId,
            final UUID reservationId) {

        // Create email log
        final EmailLog emailLog = createEmailLog(recipientEmail, subject, body, emailType, userId, reservationId);

        try {
            // Send email
            final MimeMessage message = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML content

            mailSender.send(message);

            // Mark as sent
            emailLog.setStatus(EmailStatus.SENT);
            emailLogRepository.save(emailLog);

            log.info("Email sent successfully: {} to {}", emailType, recipientEmail);

        } catch (MailException | MessagingException e) {
            // Log failure but don't throw exception (async)
            emailLog.setStatus(EmailStatus.FAILED);
            emailLog.setErrorMessage(e.getMessage());
            emailLogRepository.save(emailLog);

            log.error("Failed to send email: {} to {}", emailType, recipientEmail, e);
        }
    }

    /**
     * Retry sending a failed email.
     */
    private void retryEmail(final EmailLog emailLog) {
        log.debug("Retrying email: {} (attempt {})", emailLog.getId(), emailLog.getRetryCount() + 1);

        emailLog.setRetryCount((short) (emailLog.getRetryCount() + 1));

        try {
            final MimeMessage message = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(emailLog.getRecipientEmail());
            helper.setSubject(emailLog.getSubject());
            helper.setText(emailLog.getEmailBody(), true);

            mailSender.send(message);

            emailLog.setStatus(EmailStatus.SENT);
            log.info("Email retry successful: {}", emailLog.getId());

        } catch (MailException | MessagingException e) {
            emailLog.setErrorMessage(e.getMessage());
            log.warn("Email retry failed: {} (attempt {})", emailLog.getId(), emailLog.getRetryCount());
        }

        emailLogRepository.save(emailLog);
    }

    /**
     * Create email log entry.
     */
    private EmailLog createEmailLog(
            final String recipientEmail,
            final String subject,
            final String body,
            final EmailType emailType,
            final UUID userId,
            final UUID reservationId) {

        final EmailLog emailLog = new EmailLog();
        emailLog.setRecipientEmail(recipientEmail);
        emailLog.setSubject(subject);
        emailLog.setEmailBody(body);
        emailLog.setEmailType(emailType);
        emailLog.setUserId(userId);
        emailLog.setReservationId(reservationId);
        emailLog.setStatus(EmailStatus.PENDING);

        return emailLogRepository.save(emailLog);
    }

    // ========== Email Template Builders ==========

    /**
     * Build booking confirmation email HTML.
     */
    private String buildBookingConfirmationEmail(final Reservation reservation) {
        final String movieTitle = reservation.getShowtime().getMovie().getTitle();
        final String showtime = reservation.getShowtime().getStartTime().atZone(java.time.ZoneId.systemDefault())
                .format(DATE_FORMATTER);
        final String qrCode = generateSimpleQRCode(reservation.getBookingReference());

        final StringBuilder seats = new StringBuilder();
        for (final ReservationSeat rs : reservation.getReservationSeats()) {
            seats.append(String.format("%s%d (%s), ",
                    rs.getSeatInstance().getRowLabel(),
                    rs.getSeatInstance().getSeatNumber(),
                    rs.getSeatInstance().getType()));
        }

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2 style="color: #4CAF50;">🎬 Booking Confirmed!</h2>

                    <p>Dear %s,</p>

                    <p>Your movie ticket booking has been confirmed.</p>

                    <div style="background: #f5f5f5; padding: 20px; border-radius: 5px; margin: 20px 0;">
                        <h3>Booking Details</h3>
                        <p><strong>Booking Reference:</strong> %s</p>
                        <p><strong>Movie:</strong> %s</p>
                        <p><strong>Showtime:</strong> %s</p>
                        <p><strong>Screen:</strong> %d</p>
                        <p><strong>Seats:</strong> %s</p>
                        <p><strong>Total Amount:</strong> $%.2f</p>
                    </div>

                    <div style="background: #fff; border: 2px solid #ddd; padding: 15px; text-align: center;">
                        <p><strong>QR Code:</strong></p>
                        <pre style="font-size: 12px;">%s</pre>
                        <p style="font-size: 12px; color: #666;">Show this at the entrance</p>
                    </div>

                    <p style="margin-top: 20px;">Thank you for choosing Cinema!</p>

                    <hr style="margin-top: 30px;">
                    <p style="font-size: 12px; color: #666;">
                        Questions? Contact us at support@cinema.ankt.space
                    </p>
                </body>
                </html>
                """,
                reservation.getUser().getFullName(),
                reservation.getBookingReference(),
                movieTitle,
                showtime,
                reservation.getShowtime().getScreenNumber(),
                seats.toString(),
                reservation.getTotalPrice(),
                qrCode);
    }

    /**
     * Build cancellation email HTML.
     */
    private String buildCancellationEmail(final Reservation reservation) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2 style="color: #FF9800;">Booking Cancelled</h2>

                    <p>Dear %s,</p>

                    <p>Your booking <strong>%s</strong> has been cancelled as requested.</p>

                    <p>If payment was made, the refund will be processed within 5-7 business days.</p>

                    <p>Thank you for using Cinema!</p>
                </body>
                </html>
                """,
                reservation.getUser().getFullName(),
                reservation.getBookingReference());
    }

    /**
     * Build refund notification email HTML.
     */
    private String buildRefundEmail(final Reservation reservation, final String reason) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2 style="color: #2196F3;">Refund Processed</h2>

                    <p>Dear %s,</p>

                    <p>Your booking <strong>%s</strong> has been refunded.</p>

                    <p><strong>Reason:</strong> %s</p>

                    <p><strong>Refund Amount:</strong> $%.2f</p>

                    <p>The amount will be credited to your original payment method within 5-7 business days.</p>

                    <p>We apologize for any inconvenience.</p>
                </body>
                </html>
                """,
                reservation.getUser().getFullName(),
                reservation.getBookingReference(),
                reason,
                reservation.getTotalPrice());
    }

    /**
     * Build payment failure email HTML.
     */
    private String buildPaymentFailureEmail(final Reservation reservation, final String errorMessage) {
        return String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head><meta charset="UTF-8"></head>
                        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                            <h2 style="color: #F44336;">Payment Failed</h2>

                            <p>Dear %s,</p>

                            <p>Your payment for booking <strong>%s</strong> could not be processed.</p>

                            <p><strong>Error:</strong> %s</p>

                            <p>Your seats are still held for 5 minutes. Please try again.</p>

                            <p><a href="https://cinema.ankt.space/payment/%s" style="background: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Retry Payment</a></p>
                        </body>
                        </html>
                        """,
                reservation.getUser().getFullName(),
                reservation.getBookingReference(),
                errorMessage,
                reservation.getId());
    }

    /**
     * Generate simple ASCII QR code representation.
     * In production, use actual QR code library (e.g., ZXing).
     */
    private String generateSimpleQRCode(final String bookingReference) {
        // Simple text-based QR code placeholder
        return String.format("""
                ███████████████████████████
                ██ ▄▄▄▄▄ █ %s █ ▄▄▄▄▄ ██
                ██ █   █ █▄▄▄▄▄▄▄█ █   █ ██
                ██ █▄▄▄█ █ ▄▄▄▄▄ █ █▄▄▄█ ██
                ██▄▄▄▄▄▄▄█▄█▄█▄█▄█▄▄▄▄▄▄▄██
                ███████████████████████████
                """, bookingReference);
    }
}