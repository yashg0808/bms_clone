package com.bookmyshow.booking.scheduled;

import com.bookmyshow.booking.model.Booking;
import com.bookmyshow.booking.model.BookingStatus;
import com.bookmyshow.booking.model.SeatStatus;
import com.bookmyshow.booking.model.ShowSeat;
import com.bookmyshow.booking.repository.BookingRepository;
import com.bookmyshow.booking.repository.ShowSeatRepository;
import com.bookmyshow.booking.service.SeatCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Scheduled job that releases expired seat locks.
 * Runs every 60 seconds to clean up seats that were locked but never booked.
 * This is a safety net - Redis TTL also handles lock expiry,
 * but this ensures DB state is consistent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LockExpiryJob {

    private final ShowSeatRepository showSeatRepository;
    private final BookingRepository bookingRepository;
    private final SeatCacheService seatCacheService;

    @Value("${booking.seat-lock.timeout-minutes:8}")
    private int seatLockTimeoutMinutes;

    @Scheduled(fixedRate = 60000) // Every 60 seconds
    @Transactional
    public void releaseExpiredLocks() {
        LocalDateTime expiryThreshold = LocalDateTime.now().minusMinutes(seatLockTimeoutMinutes);

        // Collect affected show IDs before bulk update so we can evict their caches
        Set<UUID> affectedShowIds = new HashSet<>();
        List<ShowSeat> expiredSeats = showSeatRepository.findExpiredLocks(expiryThreshold);
        for (ShowSeat seat : expiredSeats) {
            affectedShowIds.add(seat.getShowId());
        }

        // Release expired seat locks
        int releasedSeats = showSeatRepository.releaseExpiredLocks(expiryThreshold);
        if (releasedSeats > 0) {
            log.info("Released {} expired seat locks (locked before {})", releasedSeats, expiryThreshold);
            // Evict cache for all affected shows
            for (UUID showId : affectedShowIds) {
                seatCacheService.evictShow(showId);
            }
        }

        // Expire pending bookings
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(now);
        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.EXPIRED);
            booking.setLockToken(null);
            affectedShowIds.add(booking.getShowId());
            log.info("Expired booking: {}", booking.getBookingNumber());
        }
        if (!expiredBookings.isEmpty()) {
            bookingRepository.saveAll(expiredBookings);
            log.info("Expired {} pending bookings", expiredBookings.size());
        }
    }
}
