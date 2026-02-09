package com.bookmyshow.shared.util;

import org.apache.commons.lang3.StringUtils;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Utility class for common validations.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[1-9]\\d{7,14}$");

    private ValidationUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Validate email format.
     */
    public static boolean isValidEmail(String email) {
        return StringUtils.isNotBlank(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate phone number format.
     */
    public static boolean isValidPhone(String phone) {
        return StringUtils.isNotBlank(phone) && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Validate UUID string.
     */
    public static boolean isValidUUID(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return false;
        }
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Parse a UUID string safely.
     */
    public static UUID parseUUID(String uuid) {
        if (!isValidUUID(uuid)) {
            throw new IllegalArgumentException("Invalid UUID: " + uuid);
        }
        return UUID.fromString(uuid);
    }

    /**
     * Validate password strength (min 8 chars, uppercase, lowercase, digit).
     */
    public static boolean isStrongPassword(String password) {
        if (StringUtils.isBlank(password) || password.length() < 8) {
            return false;
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        return hasUpper && hasLower && hasDigit;
    }
}
