package com.bookmyshow.booking.controller;

import com.bookmyshow.booking.dto.BookingResponse;
import com.bookmyshow.booking.dto.ConfirmBookingRequest;
import com.bookmyshow.booking.dto.LockSeatsRequest;
import com.bookmyshow.booking.dto.LockSeatsResponse;
import com.bookmyshow.booking.service.BookingService;
import com.bookmyshow.shared.dto.ApiResponse;
import com.bookmyshow.shared.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * Step 1: Lock seats and create a pending booking.
     * POST /api/v1/bookings/lock
     */
    @PostMapping("/lock")
    public ResponseEntity<ApiResponse<LockSeatsResponse>> lockSeats(
            @Valid @RequestBody LockSeatsRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        if (userId == null) userId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        LockSeatsResponse response = bookingService.lockSeatsAndCreateBooking(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Seats locked successfully"));
    }

    /**
     * Step 2: Confirm booking after payment.
     * POST /api/v1/bookings/confirm
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @Valid @RequestBody ConfirmBookingRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        if (userId == null) userId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        BookingResponse response = bookingService.confirmBooking(
                request.getBookingId(), request.getLockToken(), userId
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Booking confirmed successfully"));
    }

    /**
     * Cancel a booking.
     * POST /api/v1/bookings/{bookingId}/cancel
     */
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable UUID bookingId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        if (userId == null) userId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        BookingResponse response = bookingService.cancelBooking(bookingId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Booking cancelled successfully"));
    }

    /**
     * Get booking by ID.
     * GET /api/v1/bookings/{bookingId}
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable UUID bookingId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        if (userId == null) userId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        BookingResponse response = bookingService.getBooking(bookingId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get booking by booking number.
     * GET /api/v1/bookings/number/{bookingNumber}
     */
    @GetMapping("/number/{bookingNumber}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByNumber(
            @PathVariable String bookingNumber,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        if (userId == null) userId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        BookingResponse response = bookingService.getBookingByNumber(bookingNumber, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get user's booking history.
     * GET /api/v1/bookings/my
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getMyBookings(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (userId == null) userId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        Page<BookingResponse> bookings = bookingService.getUserBookings(userId, page, size);
        PageResponse<BookingResponse> pageResponse = PageResponse.<BookingResponse>builder()
                .content(bookings.getContent())
                .page(bookings.getNumber())
                .size(bookings.getSize())
                .totalElements(bookings.getTotalElements())
                .totalPages(bookings.getTotalPages())
                .first(bookings.isFirst())
                .last(bookings.isLast())
                .build();
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }
}
