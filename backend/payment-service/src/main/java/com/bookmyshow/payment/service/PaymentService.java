package com.bookmyshow.payment.service;

import com.bookmyshow.payment.dto.InitiatePaymentRequest;
import com.bookmyshow.payment.dto.PaymentResponse;
import com.bookmyshow.payment.dto.VerifyPaymentRequest;
import com.bookmyshow.payment.gateway.PaymentGateway;
import com.bookmyshow.payment.gateway.RazorpayGateway;
import com.bookmyshow.payment.gateway.StripeGateway;
import com.bookmyshow.payment.model.Payment;
import com.bookmyshow.payment.model.PaymentMethod;
import com.bookmyshow.payment.model.PaymentStatus;
import com.bookmyshow.payment.repository.PaymentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final IdempotencyService idempotencyService;
    private final RazorpayGateway razorpayGateway;
    private final StripeGateway stripeGateway;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private final Map<String, PaymentGateway> gateways = new HashMap<>();

    private static final String KAFKA_TOPIC_PAYMENT_SUCCESS = "payment.success";
    private static final String KAFKA_TOPIC_PAYMENT_FAILED = "payment.failed";
    private static final String KAFKA_TOPIC_REFUND_INITIATED = "payment.refund.initiated";

    @PostConstruct
    void initGateways() {
        gateways.put("RAZORPAY", razorpayGateway);
        gateways.put("STRIPE", stripeGateway);
    }

    /**
     * Initiate a payment. Idempotent - if same idempotency key is used, returns existing payment.
     */
    @Transactional
    public PaymentResponse initiatePayment(InitiatePaymentRequest request, UUID userId) {
        // Check idempotency
        if (idempotencyService.isDuplicate(request.getIdempotencyKey())) {
            String existingPaymentId = idempotencyService.getStoredResult(request.getIdempotencyKey());
            if (existingPaymentId != null) {
                log.info("Duplicate payment request detected. Returning existing payment: {}", existingPaymentId);
                Payment existing = paymentRepository.findById(UUID.fromString(existingPaymentId))
                        .orElseThrow(() -> new RuntimeException("Payment not found for idempotency key"));
                return mapToResponse(existing);
            }
        }

        // Resolve gateway
        String gatewayName = resolveGateway(request.getPaymentMethod());
        PaymentGateway gateway = gateways.get(gatewayName);
        if (gateway == null) {
            throw new IllegalArgumentException("Unsupported payment method: " + request.getPaymentMethod());
        }

        // Create order at gateway
        Map<String, String> metadata = Map.of(
                "bookingId", request.getBookingId().toString(),
                "userId", userId.toString()
        );
        String gatewayOrderId = gateway.createOrder(request.getAmount(), "INR", metadata);

        // Create payment record
        Payment payment = Payment.builder()
                .bookingId(request.getBookingId())
                .userId(userId)
                .amount(request.getAmount())
                .status(PaymentStatus.INITIATED)
                .paymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()))
                .gatewayOrderId(gatewayOrderId)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        payment = paymentRepository.save(payment);

        // Store idempotency mapping
        idempotencyService.storeResult(request.getIdempotencyKey(), payment.getId().toString());

        log.info("Payment initiated - paymentId: {}, bookingId: {}, gateway: {}, orderId: {}",
                payment.getId(), request.getBookingId(), gatewayName, gatewayOrderId);

        return mapToResponse(payment);
    }

    /**
     * Verify and complete a payment after gateway callback.
     */
    @Transactional
    public PaymentResponse verifyPayment(VerifyPaymentRequest request, UUID userId) {
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found: " + request.getPaymentId()));

        if (!payment.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized payment verification");
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment already verified: {}", payment.getId());
            return mapToResponse(payment);
        }

        // Resolve gateway and verify
        String gatewayName = resolveGateway(payment.getPaymentMethod().name());
        PaymentGateway gateway = gateways.get(gatewayName);

        boolean verified = gateway.verifyPayment(
                payment.getGatewayOrderId(),
                request.getGatewayPaymentId(),
                request.getGatewaySignature()
        );

        if (verified) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setGatewayPaymentId(request.getGatewayPaymentId());
            payment.setGatewaySignature(request.getGatewaySignature());
            paymentRepository.save(payment);

            // Publish success event
            publishPaymentEvent(KAFKA_TOPIC_PAYMENT_SUCCESS, payment);

            log.info("Payment verified successfully - paymentId: {}, bookingId: {}", payment.getId(), payment.getBookingId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment signature verification failed");
            paymentRepository.save(payment);

            // Publish failure event
            publishPaymentEvent(KAFKA_TOPIC_PAYMENT_FAILED, payment);

            log.warn("Payment verification failed - paymentId: {}", payment.getId());
        }

        return mapToResponse(payment);
    }

    /**
     * Initiate a refund for a payment.
     */
    @Transactional
    public PaymentResponse initiateRefund(UUID paymentId, UUID userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Can only refund successful payments. Current status: " + payment.getStatus());
        }

        String gatewayName = resolveGateway(payment.getPaymentMethod().name());
        PaymentGateway gateway = gateways.get(gatewayName);

        String refundId = gateway.initiateRefund(payment.getGatewayPaymentId(), payment.getAmount());

        payment.setStatus(PaymentStatus.REFUND_INITIATED);
        payment.setRefundId(refundId);
        payment.setRefundAmount(payment.getAmount());
        paymentRepository.save(payment);

        // Publish refund event
        publishPaymentEvent(KAFKA_TOPIC_REFUND_INITIATED, payment);

        log.info("Refund initiated - paymentId: {}, refundId: {}, amount: {}", paymentId, refundId, payment.getAmount());

        return mapToResponse(payment);
    }

    /**
     * Get payment by ID.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId, UUID userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (!payment.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to payment");
        }

        return mapToResponse(payment);
    }

    /**
     * Get payment by booking ID.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBooking(UUID bookingId, UUID userId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment not found for booking: " + bookingId));

        if (!payment.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to payment");
        }

        return mapToResponse(payment);
    }

    // ---- Private helpers ----

    private String resolveGateway(String paymentMethod) {
        return switch (paymentMethod.toUpperCase()) {
            case "STRIPE", "CREDIT_CARD" -> "STRIPE";
            default -> "RAZORPAY";
        };
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .gatewayOrderId(payment.getGatewayOrderId())
                .gatewayPaymentId(payment.getGatewayPaymentId())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private void publishPaymentEvent(String topic, Payment payment) {
        try {
            String eventPayload = String.format(
                    "{\"paymentId\":\"%s\",\"bookingId\":\"%s\",\"userId\":\"%s\",\"status\":\"%s\",\"amount\":%s}",
                    payment.getId(), payment.getBookingId(), payment.getUserId(),
                    payment.getStatus(), payment.getAmount()
            );
            kafkaTemplate.send(topic, payment.getId().toString(), eventPayload);
        } catch (Exception e) {
            log.error("Failed to publish payment event to {}: {}", topic, e.getMessage());
        }
    }
}
