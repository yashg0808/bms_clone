package com.bookmyshow.payment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Webhook controller for receiving payment gateway callbacks.
 * In production, these endpoints should validate gateway signatures
 * and update payment status accordingly.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    @PostMapping("/razorpay")
    public ResponseEntity<Map<String, String>> razorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) {
        log.info("Received Razorpay webhook - signature: {}", signature);
        log.debug("Razorpay webhook payload: {}", payload);

        // In production:
        // 1. Verify signature using Razorpay webhook secret
        // 2. Parse event type (payment.captured, payment.failed, refund.processed)
        // 3. Update payment status accordingly
        // 4. Publish Kafka event for booking service

        return ResponseEntity.ok(Map.of("status", "received"));
    }

    @PostMapping("/stripe")
    public ResponseEntity<Map<String, String>> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature
    ) {
        log.info("Received Stripe webhook - signature: {}", signature);
        log.debug("Stripe webhook payload: {}", payload);

        // In production:
        // 1. Construct event from payload + signature using Stripe SDK
        // 2. Handle event types (payment_intent.succeeded, charge.refunded, etc.)
        // 3. Update payment status
        // 4. Publish Kafka event

        return ResponseEntity.ok(Map.of("status", "received"));
    }
}
