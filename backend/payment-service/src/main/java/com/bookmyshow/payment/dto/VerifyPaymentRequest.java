package com.bookmyshow.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPaymentRequest {

    @NotNull(message = "Payment ID is required")
    private UUID paymentId;

    @NotBlank(message = "Gateway payment ID is required")
    private String gatewayPaymentId;

    @NotBlank(message = "Gateway signature is required")
    private String gatewaySignature;
}
