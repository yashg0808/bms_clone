package com.bookmyshow.booking.exception;

public class InvalidLockTokenException extends RuntimeException {

    public InvalidLockTokenException(String message) {
        super(message);
    }
}
