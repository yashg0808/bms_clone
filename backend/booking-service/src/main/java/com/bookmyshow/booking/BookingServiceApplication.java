package com.bookmyshow.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Booking Service Application - handles seat locking, booking creation,
 * and booking lifecycle management. This is the most critical service
 * with distributed concurrency control to prevent double-booking.
 */
@SpringBootApplication(scanBasePackages = {"com.bookmyshow.booking", "com.bookmyshow.shared"})
@EnableJpaAuditing
@EnableScheduling
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
