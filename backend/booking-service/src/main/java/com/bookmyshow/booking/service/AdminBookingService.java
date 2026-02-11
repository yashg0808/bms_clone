package com.bookmyshow.booking.service;

import com.bookmyshow.booking.dto.BookingResponse;
import com.bookmyshow.booking.dto.BookingStats;
import com.bookmyshow.booking.dto.PagedResponse;
import com.bookmyshow.booking.model.Booking;
import com.bookmyshow.booking.model.BookingStatus;
import com.bookmyshow.booking.repository.BookingRepository;
import com.bookmyshow.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin service for booking management.
 */
@Service
@RequiredArgsConstructor
public class AdminBookingService {

    private static final Logger log = LoggerFactory.getLogger(AdminBookingService.class);

    private final BookingRepository bookingRepository;
    private final SeatCacheService seatCacheService;

    /**
     * Get all bookings with pagination and optional filtering.
     */
    public PagedResponse<BookingResponse> getAllBookings(int page, int size, 
                                                          BookingStatus status, 
                                                          String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<Booking> bookings;
        if (search != null && !search.isBlank()) {
            bookings = bookingRepository.searchBookings(search.trim(), pageable);
        } else if (status != null) {
            bookings = bookingRepository.findByStatus(status, pageable);
        } else {
            bookings = bookingRepository.findAll(pageable);
        }
        
        Page<BookingResponse> pageResult = bookings.map(this::mapToResponse);
        return toPagedResponse(pageResult);
    }

    /**
     * Get bookings for a specific date range.
     */
    public PagedResponse<BookingResponse> getBookingsByDateRange(LocalDate startDate, 
                                                                   LocalDate endDate, 
                                                                   int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        
        Page<BookingResponse> pageResult = bookingRepository
                .findByDateRange(start, end, pageable)
                .map(this::mapToResponse);
        
        return toPagedResponse(pageResult);
    }

    /**
     * Get a booking by ID.
     */
    public BookingResponse getBookingById(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        return mapToResponse(booking);
    }

    /**
     * Get a booking by booking number.
     */
    public BookingResponse getBookingByNumber(String bookingNumber) {
        Booking booking = bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "bookingNumber", bookingNumber));
        return mapToResponse(booking);
    }

    /**
     * Admin cancel a booking (refund handled separately).
     */
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, String reason) {
        log.info("Admin cancelling booking: {} - Reason: {}", bookingId, reason);
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }
        
        // Release seats back to available
        Map<UUID, String> statusUpdates = new HashMap<>();
        Map<UUID, BigDecimal> priceUpdates = new HashMap<>();
        booking.getBookingSeats().forEach(bs -> {
            statusUpdates.put(bs.getShowSeatId(), "AVAILABLE");
            priceUpdates.put(bs.getShowSeatId(), bs.getPrice());
        });
        seatCacheService.updateSeatStatuses(booking.getShowId(), statusUpdates, priceUpdates);
        
        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);
        
        log.info("Booking {} cancelled by admin", bookingId);
        return mapToResponse(saved);
    }

    /**
     * Get booking statistics for dashboard.
     */
    public BookingStats getBookingStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = today.plusMonths(1).withDayOfMonth(1).atStartOfDay();
        
        return BookingStats.builder()
                .totalBookings(bookingRepository.count())
                .confirmedBookings(bookingRepository.countByStatus(BookingStatus.CONFIRMED))
                .pendingBookings(bookingRepository.countByStatus(BookingStatus.PENDING))
                .cancelledBookings(bookingRepository.countByStatus(BookingStatus.CANCELLED))
                .expiredBookings(bookingRepository.countByStatus(BookingStatus.EXPIRED))
                .bookingsToday(bookingRepository.countBookingsToday(startOfDay, endOfDay))
                .revenueToday(bookingRepository.getRevenueForDay(startOfDay, endOfDay))
                .revenueThisMonth(bookingRepository.getRevenueForMonth(startOfMonth, endOfMonth))
                .totalRevenue(calculateTotalRevenue())
                .build();
    }

    private BigDecimal calculateTotalRevenue() {
        // Simple calculation - sum all confirmed bookings
        return bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private <T> PagedResponse<T> toPagedResponse(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .showId(booking.getShowId())
                .guestName(booking.getGuestName())
                .guestEmail(booking.getGuestEmail())
                .guestPhone(booking.getGuestPhone())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .convenienceFee(booking.getConvenienceFee())
                .discount(booking.getDiscount())
                .finalAmount(booking.getFinalAmount())
                .seats(booking.getBookingSeats().stream()
                        .map(bs -> BookingResponse.BookingSeatInfo.builder()
                                .seatId(bs.getShowSeatId())
                                .seatRow(bs.getSeatRow())
                                .seatNumber(bs.getSeatNumber())
                                .seatType(bs.getSeatType())
                                .price(bs.getPrice())
                                .build())
                        .collect(Collectors.toList()))
                .expiresAt(booking.getExpiresAt())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
