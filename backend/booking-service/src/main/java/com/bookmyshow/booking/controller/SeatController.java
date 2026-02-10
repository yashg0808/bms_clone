package com.bookmyshow.booking.controller;

import com.bookmyshow.booking.dto.SeatStatusResponse;
import com.bookmyshow.booking.dto.ShowSeatDTO;
import com.bookmyshow.booking.service.BookingService;
import com.bookmyshow.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
public class SeatController {

    private final BookingService bookingService;

    /**
     * Get all seats for a show with their current status and layout info.
     * GET /api/v1/seats/show/{showId}
     */
    @GetMapping("/show/{showId}")
    public ResponseEntity<ApiResponse<List<ShowSeatDTO>>> getShowSeats(@PathVariable UUID showId) {
        List<ShowSeatDTO> seats = bookingService.getShowSeats(showId);
        return ResponseEntity.ok(ApiResponse.success(seats));
    }

    /**
     * Get lightweight seat statuses only (no layout data).
     * For CDN-decoupled frontend: layout from /layouts/screen-{id}.json, status from here.
     * GET /api/v1/seats/status/{showId}
     */
    @GetMapping("/status/{showId}")
    public ResponseEntity<ApiResponse<SeatStatusResponse>> getShowSeatStatuses(@PathVariable UUID showId) {
        SeatStatusResponse response = bookingService.getShowSeatStatuses(showId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get available seat count for a show.
     * GET /api/v1/seats/show/{showId}/availability
     */
    @GetMapping("/show/{showId}/availability")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSeatAvailability(@PathVariable UUID showId) {
        long availableCount = bookingService.getAvailableSeatCount(showId);
        Map<String, Object> availability = Map.of(
                "showId", showId,
                "availableSeats", availableCount
        );
        return ResponseEntity.ok(ApiResponse.success(availability));
    }
}
