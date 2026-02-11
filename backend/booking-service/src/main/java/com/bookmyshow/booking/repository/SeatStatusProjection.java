package com.bookmyshow.booking.repository;

import com.bookmyshow.booking.model.SeatStatus;

import java.util.UUID;

/**
 * JPA Projection interface for fetching only seat ID and status.
 * Used for efficient availability checks without loading the full ShowSeat entity.
 */
public interface SeatStatusProjection {
    
    UUID getId();
    
    SeatStatus getStatus();
}
