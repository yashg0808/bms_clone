package com.bookmyshow.payment.gateway;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Payment gateway interface - Strategy pattern for multiple payment providers.
 * Implementations: RazorpayGateway, StripeGateway
 */
public interface PaymentGateway {

    /**
     * Create an order/payment intent at the gateway.
     *
     * @param amount   amount in smallest currency unit
     * @param currency currency code (e.g., INR, USD)
     * @param metadata additional metadata
     * @return gateway-specific order/intent ID
     */
    String createOrder(BigDecimal amount, String currency, Map<String, String> metadata);

    /**
     * Verify payment signature from gateway webhook/callback.
     *
     * @param orderId        gateway order ID
     * @param paymentId      gateway payment ID
     * @param signature      signature from gateway
     * @return true if signature is valid
     */
    boolean verifyPayment(String orderId, String paymentId, String signature);

    /**
     * Initiate a refund for a payment.
     *
     * @param paymentId gateway payment ID
     * @param amount    refund amount
     * @return refund ID from gateway
     */
    String initiateRefund(String paymentId, BigDecimal amount);

    /**
     * Get the gateway name.
     */
    String getGatewayName();
}
