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

import java.math.BigDecimal;
import java.time.LocalDate;
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
    
    // Admin queries
    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);
    
    @Query("SELECT b FROM Booking b WHERE b.createdAt >= :startDate AND b.createdAt < :endDate")
    Page<Booking> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate, 
                                   Pageable pageable);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status")
    long countByStatus(@Param("status") BookingStatus status);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.createdAt >= :startOfDay AND b.createdAt < :endOfDay")
    long countBookingsToday(@Param("startOfDay") LocalDateTime startOfDay, 
                            @Param("endOfDay") LocalDateTime endOfDay);
    
    @Query("SELECT COALESCE(SUM(b.finalAmount), 0) FROM Booking b WHERE b.status = 'CONFIRMED' " +
           "AND b.createdAt >= :startOfDay AND b.createdAt < :endOfDay")
    BigDecimal getRevenueForDay(@Param("startOfDay") LocalDateTime startOfDay, 
                                 @Param("endOfDay") LocalDateTime endOfDay);
    
    @Query("SELECT COALESCE(SUM(b.finalAmount), 0) FROM Booking b WHERE b.status = 'CONFIRMED' " +
           "AND b.createdAt >= :startOfMonth AND b.createdAt < :endOfMonth")
    BigDecimal getRevenueForMonth(@Param("startOfMonth") LocalDateTime startOfMonth, 
                                   @Param("endOfMonth") LocalDateTime endOfMonth);
    
    @Query("SELECT b FROM Booking b WHERE " +
           "(LOWER(b.guestEmail) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(b.guestName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(b.guestPhone) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(b.bookingNumber) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Booking> searchBookings(@Param("query") String query, Pageable pageable);
}
