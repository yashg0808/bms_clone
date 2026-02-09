package com.bookmyshow.booking.controller;

import com.bookmyshow.booking.model.ShowSeat;
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
     * Get all seats for a show with their current status.
     * GET /api/v1/seats/show/{showId}
     */
    @GetMapping("/show/{showId}")
    public ResponseEntity<ApiResponse<List<ShowSeat>>> getShowSeats(@PathVariable UUID showId) {
        List<ShowSeat> seats = bookingService.getShowSeats(showId);
        return ResponseEntity.ok(ApiResponse.success(seats));
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
