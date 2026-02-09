package com.bookmyshow.notification.service;

import com.bookmyshow.notification.model.*;
import com.bookmyshow.notification.repository.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final ObjectMapper objectMapper;

    /**
     * Process a booking confirmed event.
     */
    public void handleBookingConfirmed(String eventPayload) {
        try {
            JsonNode event = objectMapper.readTree(eventPayload);
            UUID bookingId = UUID.fromString(event.get("bookingId").asText());
            String bookingNumber = event.get("bookingNumber").asText();
            UUID userId = UUID.fromString(event.get("userId").asText());
            String finalAmount = event.get("finalAmount").asText();
            int seatCount = event.get("seatCount").asInt();

            // Create and save notification record
            Notification notification = Notification.builder()
                    .userId(userId)
                    .type(NotificationType.BOOKING_CONFIRMED)
                    .channel(NotificationChannel.EMAIL)
                    .subject("Booking Confirmed - " + bookingNumber)
                    .body(buildBookingConfirmedBody(bookingNumber, finalAmount, seatCount))
                    .recipient(userId.toString()) // In production, fetch user email
                    .bookingId(bookingId)
                    .status(NotificationStatus.PENDING)
                    .build();

            notification = notificationRepository.save(notification);

            // Send email
            try {
                emailService.sendSimpleEmail(
                        notification.getRecipient(),
                        notification.getSubject(),
                        notification.getBody()
                );
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
            } catch (Exception e) {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setErrorMessage(e.getMessage());
                log.error("Failed to send booking confirmation email: {}", e.getMessage());
            }

            notificationRepository.save(notification);
            log.info("Processed booking confirmed notification for booking: {}", bookingNumber);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse booking confirmed event: {}", e.getMessage());
        }
    }

    /**
     * Process a booking cancelled event.
     */
    public void handleBookingCancelled(String eventPayload) {
        try {
            JsonNode event = objectMapper.readTree(eventPayload);
            UUID bookingId = UUID.fromString(event.get("bookingId").asText());
            String bookingNumber = event.get("bookingNumber").asText();
            UUID userId = UUID.fromString(event.get("userId").asText());

            Notification notification = Notification.builder()
                    .userId(userId)
                    .type(NotificationType.BOOKING_CANCELLED)
                    .channel(NotificationChannel.EMAIL)
                    .subject("Booking Cancelled - " + bookingNumber)
                    .body(buildBookingCancelledBody(bookingNumber))
                    .recipient(userId.toString())
                    .bookingId(bookingId)
                    .status(NotificationStatus.PENDING)
                    .build();

            notification = notificationRepository.save(notification);

            try {
                emailService.sendSimpleEmail(
                        notification.getRecipient(),
                        notification.getSubject(),
                        notification.getBody()
                );
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
            } catch (Exception e) {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setErrorMessage(e.getMessage());
            }

            notificationRepository.save(notification);
            log.info("Processed booking cancelled notification for booking: {}", bookingNumber);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse booking cancelled event: {}", e.getMessage());
        }
    }

    /**
     * Process a payment success event.
     */
    public void handlePaymentSuccess(String eventPayload) {
        try {
            JsonNode event = objectMapper.readTree(eventPayload);
            UUID bookingId = UUID.fromString(event.get("bookingId").asText());
            UUID userId = UUID.fromString(event.get("userId").asText());
            String amount = event.get("amount").asText();

            Notification notification = Notification.builder()
                    .userId(userId)
                    .type(NotificationType.PAYMENT_SUCCESS)
                    .channel(NotificationChannel.EMAIL)
                    .subject("Payment Successful - ₹" + amount)
                    .body(buildPaymentSuccessBody(amount, bookingId.toString()))
                    .recipient(userId.toString())
                    .bookingId(bookingId)
                    .status(NotificationStatus.PENDING)
                    .build();

            notification = notificationRepository.save(notification);

            try {
                emailService.sendSimpleEmail(
                        notification.getRecipient(),
                        notification.getSubject(),
                        notification.getBody()
                );
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
            } catch (Exception e) {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setErrorMessage(e.getMessage());
            }

            notificationRepository.save(notification);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse payment success event: {}", e.getMessage());
        }
    }

    // ---- Template builders ----

    private String buildBookingConfirmedBody(String bookingNumber, String amount, int seatCount) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: #e23744; color: white; padding: 20px; text-align: center;">
                    <h1>🎬 Booking Confirmed!</h1>
                </div>
                <div style="padding: 20px;">
                    <p>Your booking has been confirmed successfully!</p>
                    <table style="width: 100%%; border-collapse: collapse;">
                        <tr><td style="padding: 8px; font-weight: bold;">Booking Number:</td><td style="padding: 8px;">%s</td></tr>
                        <tr><td style="padding: 8px; font-weight: bold;">Seats:</td><td style="padding: 8px;">%d</td></tr>
                        <tr><td style="padding: 8px; font-weight: bold;">Total Amount:</td><td style="padding: 8px;">₹%s</td></tr>
                    </table>
                    <p style="margin-top: 20px;">Please show this confirmation at the theater entrance.</p>
                    <p>Enjoy the movie! 🍿</p>
                </div>
                <div style="background: #f5f5f5; padding: 15px; text-align: center; font-size: 12px; color: #666;">
                    <p>This is an automated notification from BookMyShow Clone</p>
                </div>
            </body>
            </html>
            """, bookingNumber, seatCount, amount);
    }

    private String buildBookingCancelledBody(String bookingNumber) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: #e23744; color: white; padding: 20px; text-align: center;">
                    <h1>Booking Cancelled</h1>
                </div>
                <div style="padding: 20px;">
                    <p>Your booking <strong>%s</strong> has been cancelled.</p>
                    <p>If a payment was made, a refund will be initiated within 5-7 business days.</p>
                </div>
            </body>
            </html>
            """, bookingNumber);
    }

    private String buildPaymentSuccessBody(String amount, String bookingId) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: #28a745; color: white; padding: 20px; text-align: center;">
                    <h1>✅ Payment Successful</h1>
                </div>
                <div style="padding: 20px;">
                    <p>Your payment of <strong>₹%s</strong> has been received successfully.</p>
                    <p>Your booking is being confirmed.</p>
                </div>
            </body>
            </html>
            """, amount);
    }
}
