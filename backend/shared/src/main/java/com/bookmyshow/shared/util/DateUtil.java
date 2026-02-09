package com.bookmyshow.shared.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility class for date/time operations.
 */
public final class DateUtil {

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    public static final DateTimeFormatter DISPLAY_TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");

    private DateUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Parse a date string in yyyy-MM-dd format.
     */
    public static LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Expected: yyyy-MM-dd, got: " + dateStr);
        }
    }

    /**
     * Format a LocalDate to display format (e.g., "15 Jan 2026").
     */
    public static String formatForDisplay(LocalDate date) {
        return date.format(DISPLAY_DATE_FORMAT);
    }

    /**
     * Check if a date is in the future.
     */
    public static boolean isFutureDate(LocalDate date) {
        return date.isAfter(LocalDate.now());
    }

    /**
     * Check if a datetime is in the past.
     */
    public static boolean isPast(LocalDateTime dateTime) {
        return dateTime.isBefore(LocalDateTime.now());
    }

    /**
     * Generate a booking number with date prefix.
     */
    public static String generateBookingNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
        return "BMS-" + datePrefix + "-" + randomSuffix;
    }
}
