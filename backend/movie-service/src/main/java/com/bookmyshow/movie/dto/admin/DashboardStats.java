package com.bookmyshow.movie.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dashboard statistics for admin panel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {

    private long totalMovies;
    private long activeMovies;
    private long totalTheaters;
    private long totalScreens;
    private long totalShows;
    private long showsToday;

    // These would come from booking-service
    private long totalBookings;
    private long bookingsToday;
    private BigDecimal revenueToday;
    private BigDecimal revenueThisMonth;

    private List<PopularMovie> popularMovies;
    private List<RecentBooking> recentBookings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PopularMovie {
        private String movieId;
        private String title;
        private String posterUrl;
        private long bookingCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentBooking {
        private String bookingId;
        private String movieTitle;
        private String theaterName;
        private String customerName;
        private BigDecimal amount;
        private String status;
    }
}
