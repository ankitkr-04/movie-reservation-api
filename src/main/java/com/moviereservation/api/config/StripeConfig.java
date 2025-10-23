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
    @Value("${stripe.api.secret-key}")
    private String secretKey;

    @Value("${stripe.api.public-key}")
    private String publicKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${currency:INR}")
    private String currency;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
        log.info("Stripe API key initialized successfully.");
    }

}
