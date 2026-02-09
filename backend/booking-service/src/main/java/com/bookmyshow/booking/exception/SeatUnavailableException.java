package com.bookmyshow.booking.exception;

import java.util.List;
import java.util.UUID;

public class SeatUnavailableException extends RuntimeException {

    private final List<UUID> unavailableSeatIds;

    public SeatUnavailableException(String message) {
        super(message);
        this.unavailableSeatIds = List.of();
    }

    public SeatUnavailableException(String message, List<UUID> unavailableSeatIds) {
        super(message);
        this.unavailableSeatIds = unavailableSeatIds;
    }

    public List<UUID> getUnavailableSeatIds() {
        return unavailableSeatIds;
    }
}
