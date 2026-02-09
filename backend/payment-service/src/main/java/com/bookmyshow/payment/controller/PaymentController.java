package com.bookmyshow.payment.controller;

import com.bookmyshow.payment.dto.InitiatePaymentRequest;
import com.bookmyshow.payment.dto.PaymentResponse;
import com.bookmyshow.payment.dto.VerifyPaymentRequest;
import com.bookmyshow.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<Map<String, Object>> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        PaymentResponse response = paymentService.initiatePayment(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("success", true, "data", response, "message", "Payment initiated"));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        PaymentResponse response = paymentService.verifyPayment(request, userId);
        return ResponseEntity.ok(Map.of("success", true, "data", response, "message", "Payment verified"));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<Map<String, Object>> initiateRefund(
            @PathVariable UUID paymentId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        PaymentResponse response = paymentService.initiateRefund(paymentId, userId);
        return ResponseEntity.ok(Map.of("success", true, "data", response, "message", "Refund initiated"));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<Map<String, Object>> getPayment(
            @PathVariable UUID paymentId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        PaymentResponse response = paymentService.getPayment(paymentId, userId);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<Map<String, Object>> getPaymentByBooking(
            @PathVariable UUID bookingId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        PaymentResponse response = paymentService.getPaymentByBooking(bookingId, userId);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }
}
