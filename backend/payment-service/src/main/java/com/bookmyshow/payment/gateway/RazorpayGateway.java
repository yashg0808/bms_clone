package com.bookmyshow.payment.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Razorpay payment gateway implementation.
 * In production, this would use the Razorpay Java SDK.
 * This implementation simulates the gateway for development/testing.
 */
@Slf4j
@Component
public class RazorpayGateway implements PaymentGateway {

    @Value("${payment.razorpay.key-id:rzp_test_key}")
    private String keyId;

    @Value("${payment.razorpay.key-secret:rzp_test_secret}")
    private String keySecret;

    @Override
    public String createOrder(BigDecimal amount, String currency, Map<String, String> metadata) {
        // In production: Use Razorpay SDK to create order
        // RazorpayClient client = new RazorpayClient(keyId, keySecret);
        // JSONObject orderRequest = new JSONObject();
        // orderRequest.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue());
        // orderRequest.put("currency", currency);
        // Order order = client.orders.create(orderRequest);

        String orderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        log.info("Razorpay order created: {} for amount {} {}", orderId, amount, currency);
        return orderId;
    }

    @Override
    public boolean verifyPayment(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            String expectedSignature = calculateHMAC(payload, keySecret);
            boolean valid = expectedSignature.equals(signature);
            log.info("Razorpay payment verification - orderId: {}, paymentId: {}, valid: {}", orderId, paymentId, valid);
            return valid;
        } catch (Exception e) {
            log.error("Error verifying Razorpay payment: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String initiateRefund(String paymentId, BigDecimal amount) {
        // In production: Use Razorpay SDK
        // JSONObject refundRequest = new JSONObject();
        // refundRequest.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue());
        // Refund refund = client.payments.refund(paymentId, refundRequest);

        String refundId = "rfnd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        log.info("Razorpay refund initiated: {} for payment {} amount {}", refundId, paymentId, amount);
        return refundId;
    }

    @Override
    public String getGatewayName() {
        return "RAZORPAY";
    }

    private String calculateHMAC(String data, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
