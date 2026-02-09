package com.bookmyshow.payment.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Stripe payment gateway implementation.
 * In production, this would use the Stripe Java SDK.
 * This implementation simulates the gateway for development/testing.
 */
@Slf4j
@Component
public class StripeGateway implements PaymentGateway {

    @Value("${payment.stripe.secret-key:sk_test_key}")
    private String secretKey;

    @Value("${payment.stripe.webhook-secret:whsec_test}")
    private String webhookSecret;

    @Override
    public String createOrder(BigDecimal amount, String currency, Map<String, String> metadata) {
        // In production: Use Stripe SDK
        // PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
        //     .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue())
        //     .setCurrency(currency.toLowerCase())
        //     .build();
        // PaymentIntent intent = PaymentIntent.create(params);

        String intentId = "pi_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        log.info("Stripe PaymentIntent created: {} for amount {} {}", intentId, amount, currency);
        return intentId;
    }

    @Override
    public boolean verifyPayment(String orderId, String paymentId, String signature) {
        // In production: Verify webhook signature using Stripe SDK
        // Webhook.constructEvent(payload, sigHeader, webhookSecret);
        log.info("Stripe payment verification - intentId: {}, paymentId: {}", orderId, paymentId);
        return true; // Simplified for dev
    }

    @Override
    public String initiateRefund(String paymentId, BigDecimal amount) {
        // In production: Use Stripe SDK
        // RefundCreateParams params = RefundCreateParams.builder()
        //     .setPaymentIntent(paymentId)
        //     .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue())
        //     .build();
        // Refund refund = Refund.create(params);

        String refundId = "re_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        log.info("Stripe refund initiated: {} for payment {} amount {}", refundId, paymentId, amount);
        return refundId;
    }

    @Override
    public String getGatewayName() {
        return "STRIPE";
    }
}
