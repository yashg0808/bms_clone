package com.bookmyshow.movie.controller;

import com.bookmyshow.movie.dto.CityResponse;
import com.bookmyshow.movie.dto.TheaterResponse;
import com.bookmyshow.movie.service.TheaterService;
import com.bookmyshow.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for theater and city operations.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TheaterController {

    private final TheaterService theaterService;

    /**
     * Get all active cities.
     * Cached for 24 hours.
     */
    @GetMapping("/cities")
    public ResponseEntity<ApiResponse<List<CityResponse>>> getCities() {
        return ResponseEntity.ok(ApiResponse.success(theaterService.getAllCities()));
    }

    /**
     * Get theaters in a city.
     * Cached for 6 hours.
     */
    @GetMapping("/cities/{cityId}/theaters")
    public ResponseEntity<ApiResponse<List<TheaterResponse>>> getTheatersByCity(
            @PathVariable UUID cityId) {
        return ResponseEntity.ok(ApiResponse.success(theaterService.getTheatersByCity(cityId)));
    }
}
