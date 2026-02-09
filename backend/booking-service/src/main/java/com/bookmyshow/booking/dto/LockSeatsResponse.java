package com.bookmyshow.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockSeatsResponse {

    private String lockToken;
    private UUID showId;
    private List<LockedSeatInfo> lockedSeats;
    private BigDecimal totalAmount;
    private LocalDateTime expiresAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LockedSeatInfo {
        private UUID seatId;
        private String seatRow;
        private String seatNumber;
        private String seatType;
        private BigDecimal price;
    }
}
