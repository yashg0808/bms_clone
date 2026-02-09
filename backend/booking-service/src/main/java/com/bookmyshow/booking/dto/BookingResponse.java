package com.bookmyshow.booking.dto;

import com.bookmyshow.booking.model.BookingStatus;
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
public class BookingResponse {

    private UUID id;
    private String bookingNumber;
    private UUID userId;
    private UUID showId;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private BigDecimal convenienceFee;
    private BigDecimal discount;
    private BigDecimal finalAmount;
    private String lockToken;
    private LocalDateTime expiresAt;
    private List<BookingSeatInfo> seats;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingSeatInfo {
        private UUID seatId;
        private String seatRow;
        private String seatNumber;
        private String seatType;
        private BigDecimal price;
    }
}
