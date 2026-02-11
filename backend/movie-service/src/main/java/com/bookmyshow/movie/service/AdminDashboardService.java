package com.bookmyshow.movie.service;

import com.bookmyshow.movie.dto.admin.DashboardStats;
import com.bookmyshow.movie.repository.MovieRepository;
import com.bookmyshow.movie.repository.ScreenRepository;
import com.bookmyshow.movie.repository.ShowRepository;
import com.bookmyshow.movie.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

/**
 * Service for admin dashboard analytics.
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final Logger log = LoggerFactory.getLogger(AdminDashboardService.class);

    private final MovieRepository movieRepository;
    private final TheaterRepository theaterRepository;
    private final ScreenRepository screenRepository;
    private final ShowRepository showRepository;

    /**
     * Get dashboard statistics.
     */
    public DashboardStats getDashboardStats() {
        log.debug("Fetching dashboard statistics");
        
        LocalDate today = LocalDate.now();
        
        return DashboardStats.builder()
                .totalMovies(movieRepository.count())
                .activeMovies(movieRepository.countByIsActiveTrue())
                .totalTheaters(theaterRepository.count())
                .totalScreens(screenRepository.count())
                .totalShows(showRepository.count())
                .showsToday(showRepository.countByShowDate(today))
                // Booking stats would come from booking-service via API call
                .totalBookings(0)
                .bookingsToday(0)
                .revenueToday(BigDecimal.ZERO)
                .revenueThisMonth(BigDecimal.ZERO)
                .popularMovies(Collections.emptyList())
                .recentBookings(Collections.emptyList())
                .build();
    }
}
