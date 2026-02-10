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

    List<Booking> findByShowId(UUID showId);

    Page<Booking> findByGuestEmail(String guestEmail, Pageable pageable);

    Page<Booking> findByGuestPhone(String guestPhone, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND b.expiresAt < :now")
    List<Booking> findExpiredBookings(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Booking b SET b.status = 'EXPIRED' WHERE b.status = 'PENDING' AND b.expiresAt < :now")
    int expireBookings(@Param("now") LocalDateTime now);

    boolean existsByBookingNumber(String bookingNumber);

    Page<Booking> findAll(Pageable pageable);
}
