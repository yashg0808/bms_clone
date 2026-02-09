package com.bookmyshow.booking.repository;

import com.bookmyshow.booking.model.Booking;
import com.bookmyshow.booking.model.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByBookingNumber(String bookingNumber);

    Page<Booking> findByUserId(UUID userId, Pageable pageable);

    Page<Booking> findByUserIdAndStatus(UUID userId, BookingStatus status, Pageable pageable);

    List<Booking> findByShowId(UUID showId);

    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING_PAYMENT' AND b.expiresAt < :now")
    List<Booking> findExpiredBookings(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Booking b SET b.status = 'EXPIRED' WHERE b.status = 'PENDING_PAYMENT' AND b.expiresAt < :now")
    int expireBookings(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.userId = :userId AND b.showId = :showId AND b.status IN ('PENDING_PAYMENT', 'CONFIRMED')")
    long countActiveBookingsByUserAndShow(@Param("userId") UUID userId, @Param("showId") UUID showId);

    boolean existsByBookingNumber(String bookingNumber);
}
