package com.moviereservation.api.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.moviereservation.api.config.StripeConfig;
import com.moviereservation.api.constant.PaymentConstants;
import com.moviereservation.api.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Webhook controller for Stripe payment events.
 * Handles asynchronous payment notifications from Stripe.
 * 
 * IMPORTANT: This endpoint must be publicly accessible (no authentication).
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
@Hidden // Hide from Swagger docs
public class WebhookController {

    private final PaymentService paymentService;
    private final StripeConfig stripeConfig;

    /**
     * Stripe webhook endpoint.
     * Receives payment events from Stripe and processes them.
     *
     * Events handled:
     * - payment_intent.succeeded: Payment completed successfully
     * - payment_intent.payment_failed: Payment failed
     * - payment_intent.canceled: Payment cancelled
     *
     * @param payload   Raw webhook payload
     * @param sigHeader Stripe signature header for verification
     * @return 200 OK if processed successfully
     */
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        log.debug("Received Stripe webhook event");

        Event event;

        try {
            // Verify webhook signature
            event = Webhook.constructEvent(
                    payload,
                    sigHeader,
                    stripeConfig.getWebhookSecret());

        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid signature");
        }

        // Handle idempotency: Check if event already processed
        // TODO: Implement event ID tracking to prevent duplicate processing
        // if (eventAlreadyProcessed(event.getId())) {
        // return ResponseEntity.ok("Event already processed");
        // }

        // Extract event type and data
        String eventType = event.getType();
        log.info("Processing Stripe event: {} (ID: {})", eventType, event.getId());

        try {
            // Handle different event types
            switch (eventType) {
                case PaymentConstants.EVENT_PAYMENT_INTENT_SUCCEEDED:
                case PaymentConstants.EVENT_PAYMENT_INTENT_FAILED:
                case PaymentConstants.EVENT_PAYMENT_INTENT_CANCELED:
                    handlePaymentIntentEvent(event, eventType);
                    break;

                default:
                    log.debug("Unhandled event type: {}", eventType);
            }

            return ResponseEntity.ok("Event processed successfully");

        } catch (Exception e) {
            log.error("Error processing webhook event: {}", event.getId(), e);
            // Return 200 to prevent Stripe from retrying
            // Log error for manual investigation
            return ResponseEntity.ok("Event received but processing failed");
        }
    }

    /**
     * Handle PaymentIntent-related events.
     */
    private void handlePaymentIntentEvent(Event event, String eventType) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new IllegalStateException("Failed to deserialize PaymentIntent"));

        String paymentIntentId = paymentIntent.getId();

        log.info("Processing PaymentIntent event: {} for {}", eventType, paymentIntentId);

        // Delegate to payment service
        paymentService.processWebhookEvent(paymentIntentId, eventType);
    }
}