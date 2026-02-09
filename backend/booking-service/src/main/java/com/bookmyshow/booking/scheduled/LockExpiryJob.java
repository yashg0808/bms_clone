package com.bookmyshow.booking.scheduled;

import com.bookmyshow.booking.model.Booking;
import com.bookmyshow.booking.model.BookingStatus;
import com.bookmyshow.booking.model.SeatStatus;
import com.bookmyshow.booking.model.ShowSeat;
import com.bookmyshow.booking.repository.BookingRepository;
import com.bookmyshow.booking.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    @Value("${booking.seat-lock.timeout-minutes:8}")
    private int seatLockTimeoutMinutes;

    @Scheduled(fixedRate = 60000) // Every 60 seconds
    @Transactional
    public void releaseExpiredLocks() {
        LocalDateTime expiryThreshold = LocalDateTime.now().minusMinutes(seatLockTimeoutMinutes);

        // Release expired seat locks
        int releasedSeats = showSeatRepository.releaseExpiredLocks(expiryThreshold);
        if (releasedSeats > 0) {
            log.info("Released {} expired seat locks (locked before {})", releasedSeats, expiryThreshold);
        }

        // Expire pending bookings
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(now);
        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.EXPIRED);
            booking.setLockToken(null);
            log.info("Expired booking: {}", booking.getBookingNumber());
        }
        if (!expiredBookings.isEmpty()) {
            bookingRepository.saveAll(expiredBookings);
            log.info("Expired {} pending bookings", expiredBookings.size());
        }
    }
}
