package com.bookmyshow.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request to confirm a booking by providing guest details.
 * After seats are locked, the user submits their name, email, and phone
 * to finalize the booking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmBookingRequest {

    @NotNull(message = "Booking ID is required")
    private UUID bookingId;

    @NotBlank(message = "Lock token is required")
    private String lockToken;

    @NotBlank(message = "Name is required")
    private String guestName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String guestEmail;

    @NotBlank(message = "Phone number is required")
    private String guestPhone;
}
