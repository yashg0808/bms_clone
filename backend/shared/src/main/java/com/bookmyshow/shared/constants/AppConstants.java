package com.bookmyshow.shared.constants;

/**
 * Application-wide constants shared across all BookMyShow services.
 */
public final class AppConstants {

    private AppConstants() {
        // Private constructor to prevent instantiation
    }

    // ---- Pagination ----
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_FIELD = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "desc";

    // ---- Seat Locking ----
    public static final int SEAT_LOCK_TIMEOUT_SECONDS = 10;
    public static final int SEAT_LOCK_LEASE_MINUTES = 10;
    public static final int SEAT_LOCK_EXPIRY_MINUTES = 15;

    // ---- Redis Key Prefixes ----
    public static final String REDIS_SEAT_LOCK_PREFIX = "seat:lock:";
    public static final String REDIS_LOCK_TOKEN_PREFIX = "lock:token:";
    public static final String REDIS_CACHE_MOVIE_PREFIX = "cache:movie:";
    public static final String REDIS_CACHE_SHOW_PREFIX = "cache:show:";

    // ---- Kafka Topics ----
    public static final String KAFKA_TOPIC_BOOKING_CONFIRMED = "booking.confirmed";
    public static final String KAFKA_TOPIC_BOOKING_CANCELLED = "booking.cancelled";

    // ---- HTTP Headers ----
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    // ---- Booking ----
    public static final int MAX_SEATS_PER_BOOKING = 10;
    public static final String BOOKING_NUMBER_PREFIX = "BMS";
}
