package com.moviereservation.api.constant;

/**
 * Constants for payment processing.
 */
public final class PaymentConstants {

    private PaymentConstants() {
        // Prevent instantiation
    }

    // Payment method
    public static final String PAYMENT_METHOD_STRIPE = "STRIPE";

    // Currency
    public static final String CURRENCY_USD = "USD";
    public static final String CURRENCY_INR = "INR";

    // Stripe event types
    public static final String EVENT_PAYMENT_INTENT_SUCCEEDED = "payment_intent.succeeded";
    public static final String EVENT_PAYMENT_INTENT_FAILED = "payment_intent.payment_failed";
    public static final String EVENT_PAYMENT_INTENT_CANCELED = "payment_intent.canceled";

    // Payment description format
    public static final String PAYMENT_DESCRIPTION_FORMAT = "Movie Reservation - Booking Ref: %s";

    // Refund reason
    public static final String REFUND_REASON_CUSTOMER_REQUEST = "Customer cancellation";
    public static final String REFUND_REASON_SHOWTIME_CANCELLED = "Showtime cancelled by theater";
}