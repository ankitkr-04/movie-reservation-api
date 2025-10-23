package com.moviereservation.api.web.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moviereservation.api.constant.Route;
import com.moviereservation.api.security.UserPrincipal;
import com.moviereservation.api.service.PaymentService;
import com.moviereservation.api.web.dto.response.wrappers.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Payment endpoints for initiating payments.
 * Webhook handling is in separate WebhookController for security.
 */
@RestController
@RequestMapping(Route.PAYMENTS)
@Tag(name = "Payments", description = "Payment processing endpoints")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create Stripe PaymentIntent for a reservation.
     * Returns client secret for frontend to complete payment with Stripe.js.
     *
     * @param reservationId Reservation UUID
     * @param principal     Authenticated user
     * @return Client secret for Stripe
     */
    @PostMapping("/create-intent/{reservationId}")
    @Operation(summary = "Create payment intent", description = "Initiates Stripe payment for a PENDING_PAYMENT reservation. "
            +
            "Returns client secret for Stripe.js to complete payment on frontend.")
    public ResponseEntity<ApiResponse<String>> createPaymentIntent(
            @PathVariable("reservationId") UUID reservationId,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.debug("Creating payment intent for reservation: {} by user: {}",
                reservationId, principal.getUserId());

        String clientSecret = paymentService.createPaymentIntent(
                reservationId,
                principal.getUserId());

        return ResponseEntity.ok(
                ApiResponse.success("Payment intent created successfully", clientSecret));
    }
}