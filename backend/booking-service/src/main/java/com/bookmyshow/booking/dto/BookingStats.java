package com.bookmyshow.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Dashboard statistics for admin panel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingStats {
    private long totalBookings;
    private long confirmedBookings;
    private long pendingBookings;
    private long cancelledBookings;
    private long expiredBookings;
    private long bookingsToday;
    private BigDecimal revenueToday;
    private BigDecimal revenueThisMonth;
    private BigDecimal totalRevenue;
}
