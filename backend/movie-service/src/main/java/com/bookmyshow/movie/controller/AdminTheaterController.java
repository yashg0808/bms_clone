package com.bookmyshow.movie.controller;

import com.bookmyshow.movie.dto.CityResponse;
import com.bookmyshow.movie.dto.ScreenResponse;
import com.bookmyshow.movie.dto.TheaterResponse;
import com.bookmyshow.movie.dto.admin.CreateScreenRequest;
import com.bookmyshow.movie.dto.admin.CreateTheaterRequest;
import com.bookmyshow.movie.service.AdminTheaterService;
import com.bookmyshow.movie.service.TheaterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin controller for theater and screen management.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/theaters")
@RequiredArgsConstructor
public class AdminTheaterController {

    private final AdminTheaterService adminTheaterService;
    private final TheaterService theaterService;

    // ==================== Cities ====================

    /**
     * Get all cities.
     */
    @GetMapping("/cities")
    public ResponseEntity<List<CityResponse>> getAllCities() {
        return ResponseEntity.ok(theaterService.getAllCities());
    }

    // ==================== Theaters ====================

    /**
     * Get all theaters.
     */
    @GetMapping
    public ResponseEntity<List<TheaterResponse>> getAllTheaters() {
        return ResponseEntity.ok(adminTheaterService.getAllTheaters());
    }

    /**
     * Get a theater by ID.
     */
    @GetMapping("/{theaterId}")
    public ResponseEntity<TheaterResponse> getTheater(@PathVariable UUID theaterId) {
        return ResponseEntity.ok(adminTheaterService.getTheaterById(theaterId));
    }

    /**
     * Create a new theater.
     */
    @PostMapping
    public ResponseEntity<TheaterResponse> createTheater(@Valid @RequestBody CreateTheaterRequest request) {
        TheaterResponse theater = adminTheaterService.createTheater(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(theater);
    }

    /**
     * Update a theater.
     */
    @PutMapping("/{theaterId}")
    public ResponseEntity<TheaterResponse> updateTheater(
            @PathVariable UUID theaterId,
            @RequestBody CreateTheaterRequest request) {
        return ResponseEntity.ok(adminTheaterService.updateTheater(theaterId, request));
    }

    /**
     * Delete a theater (soft delete).
     */
    @DeleteMapping("/{theaterId}")
    public ResponseEntity<Map<String, String>> deleteTheater(@PathVariable UUID theaterId) {
        adminTheaterService.deleteTheater(theaterId);
        return ResponseEntity.ok(Map.of("message", "Theater deactivated successfully"));
    }

    // ==================== Screens ====================

    /**
     * Get screens for a theater.
     */
    @GetMapping("/{theaterId}/screens")
    public ResponseEntity<List<ScreenResponse>> getScreens(@PathVariable UUID theaterId) {
        return ResponseEntity.ok(adminTheaterService.getScreensByTheater(theaterId));
    }

    /**
     * Create a new screen.
     */
    @PostMapping("/screens")
    public ResponseEntity<ScreenResponse> createScreen(@Valid @RequestBody CreateScreenRequest request) {
        ScreenResponse screen = adminTheaterService.createScreen(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(screen);
    }

    /**
     * Update a screen.
     */
    @PutMapping("/screens/{screenId}")
    public ResponseEntity<ScreenResponse> updateScreen(
            @PathVariable UUID screenId,
            @RequestBody CreateScreenRequest request) {
        return ResponseEntity.ok(adminTheaterService.updateScreen(screenId, request));
    }

    /**
     * Delete a screen (soft delete).
     */
    @DeleteMapping("/screens/{screenId}")
    public ResponseEntity<Map<String, String>> deleteScreen(@PathVariable UUID screenId) {
        adminTheaterService.deleteScreen(screenId);
        return ResponseEntity.ok(Map.of("message", "Screen deactivated successfully"));
    }
}
