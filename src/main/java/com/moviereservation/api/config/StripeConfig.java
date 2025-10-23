package com.moviereservation.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.stripe.Stripe;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@Getter

public class StripeConfig {
    @Value("${app.stripe.api.secret-key}")
    private String secretKey;

    @Value("${app.stripe.api.public-key}")
    private String publicKey;

    @Value("${app.stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${app.currency:INR}")
    private String currency;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
        log.info("Stripe API key initialized successfully.");
    }

}
