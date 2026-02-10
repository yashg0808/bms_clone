package com.bookmyshow.notification.consumer;

import com.bookmyshow.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens to booking events
 * and triggers appropriate notifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "booking.confirmed", groupId = "notification-service")
    public void onBookingConfirmed(String eventPayload) {
        log.info("Received booking.confirmed event");
        try {
            notificationService.handleBookingConfirmed(eventPayload);
        } catch (Exception e) {
            log.error("Error processing booking.confirmed event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "booking.cancelled", groupId = "notification-service")
    public void onBookingCancelled(String eventPayload) {
        log.info("Received booking.cancelled event");
        try {
            notificationService.handleBookingCancelled(eventPayload);
        } catch (Exception e) {
            log.error("Error processing booking.cancelled event: {}", e.getMessage(), e);
        }
    }
}
