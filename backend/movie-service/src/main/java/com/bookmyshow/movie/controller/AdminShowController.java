package com.bookmyshow.movie.controller;

import com.bookmyshow.movie.dto.ShowResponse;
import com.bookmyshow.movie.dto.PagedResponse;
import com.bookmyshow.movie.dto.admin.CreateShowRequest;
import com.bookmyshow.movie.dto.admin.UpdateShowRequest;
import com.bookmyshow.movie.service.AdminShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin controller for show scheduling.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/shows")
@RequiredArgsConstructor
public class AdminShowController {

    private final AdminShowService adminShowService;

    /**
     * Get all shows with optional filters.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ShowResponse>> getAllShows(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID movieId,
            @RequestParam(required = false) UUID screenId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(adminShowService.getAllShows(page, size, movieId, screenId, date));
    }

    /**
     * Get shows for a specific screen on a date.
     */
    @GetMapping("/screen/{screenId}")
    public ResponseEntity<List<ShowResponse>> getShowsByScreen(
            @PathVariable UUID screenId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(adminShowService.getShowsByScreenAndDate(screenId, date));
    }

    /**
     * Create a new show.
     */
    @PostMapping
    public ResponseEntity<ShowResponse> createShow(@Valid @RequestBody CreateShowRequest request) {
        ShowResponse show = adminShowService.createShow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(show);
    }

    /**
     * Bulk create shows.
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<ShowResponse>> bulkCreateShows(
            @Valid @RequestBody List<CreateShowRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminShowService.bulkCreateShows(requests));
    }

    /**
     * Update an existing show.
     */
    @PutMapping("/{showId}")
    public ResponseEntity<ShowResponse> updateShow(
            @PathVariable UUID showId,
            @RequestBody UpdateShowRequest request) {
        return ResponseEntity.ok(adminShowService.updateShow(showId, request));
    }

    /**
     * Delete a show (soft delete).
     */
    @DeleteMapping("/{showId}")
    public ResponseEntity<Map<String, String>> deleteShow(@PathVariable UUID showId) {
        adminShowService.deleteShow(showId);
        return ResponseEntity.ok(Map.of("message", "Show deactivated successfully"));
    }
}
