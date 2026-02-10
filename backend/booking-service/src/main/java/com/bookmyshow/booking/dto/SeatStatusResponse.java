package com.bookmyshow.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Lightweight DTO for seat status only (no layout data).
 * Used by the CDN-decoupled frontend flow: layout from static JSON, status from this API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatStatusResponse {

    private UUID showId;
    private List<SeatStatus> seats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatStatus {
        private UUID seatId;       // references seats.id (matches layout JSON seatId)
        private UUID showSeatId;   // references show_seats.id (used for lock requests)
        private String status;     // AVAILABLE, LOCKED, BOOKED
        private BigDecimal price;
    }
}
