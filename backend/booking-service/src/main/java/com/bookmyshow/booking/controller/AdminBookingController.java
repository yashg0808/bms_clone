package com.bookmyshow.booking.controller;

import com.bookmyshow.booking.dto.BookingResponse;
import com.bookmyshow.booking.dto.BookingStats;
import com.bookmyshow.booking.dto.PagedResponse;
import com.bookmyshow.booking.model.BookingStatus;
import com.bookmyshow.booking.service.AdminBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Admin controller for booking management.
 */
@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    /**
     * Get all bookings with pagination and optional filters.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<BookingResponse>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminBookingService.getAllBookings(page, size, status, search));
    }

    /**
     * Get bookings by date range.
     */
    @GetMapping("/by-date")
    public ResponseEntity<PagedResponse<BookingResponse>> getBookingsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminBookingService.getBookingsByDateRange(startDate, endDate, page, size));
    }

    /**
     * Get a specific booking by ID.
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(adminBookingService.getBookingById(bookingId));
    }

    /**
     * Get a booking by booking number.
     */
    @GetMapping("/number/{bookingNumber}")
    public ResponseEntity<BookingResponse> getBookingByNumber(@PathVariable String bookingNumber) {
        return ResponseEntity.ok(adminBookingService.getBookingByNumber(bookingNumber));
    }

    /**
     * Cancel a booking (admin action).
     */
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable UUID bookingId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : "Cancelled by admin";
        return ResponseEntity.ok(adminBookingService.cancelBooking(bookingId, reason));
    }

    /**
     * Get booking statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<BookingStats> getBookingStats() {
        return ResponseEntity.ok(adminBookingService.getBookingStats());
    }
}
