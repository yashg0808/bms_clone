package com.bookmyshow.payment.dto;

import com.bookmyshow.payment.model.PaymentMethod;
import com.bookmyshow.payment.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID id;
    private UUID bookingId;
    private UUID userId;
    private BigDecimal amount;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String gatewayOrderId;
    private String gatewayPaymentId;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
