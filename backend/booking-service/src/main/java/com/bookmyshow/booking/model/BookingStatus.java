package com.bookmyshow.booking.model;

/**
 * Booking lifecycle statuses.
 * PENDING - seats locked, waiting for guest details
 * CONFIRMED - guest details submitted, booking complete
 * CANCELLED - booking was cancelled
 * EXPIRED - lock timed out before confirmation
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    EXPIRED
}
