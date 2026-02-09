package com.bookmyshow.movie.controller;

import com.bookmyshow.movie.dto.CityResponse;
import com.bookmyshow.movie.dto.TheaterResponse;
import com.bookmyshow.movie.model.City;
import com.bookmyshow.movie.model.Theater;
import com.bookmyshow.movie.repository.CityRepository;
import com.bookmyshow.movie.repository.TheaterRepository;
import com.bookmyshow.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for theater and city operations.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TheaterController {

    private final TheaterRepository theaterRepository;
    private final CityRepository cityRepository;

    /**
     * Get all active cities.
     */
    @GetMapping("/cities")
    public ResponseEntity<ApiResponse<List<CityResponse>>> getCities() {
        List<CityResponse> cities = cityRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(c -> CityResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .state(c.getState())
                        .country(c.getCountry())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(cities));
    }

    /**
     * Get theaters in a city.
     */
    @GetMapping("/cities/{cityId}/theaters")
    public ResponseEntity<ApiResponse<List<TheaterResponse>>> getTheatersByCity(
            @PathVariable UUID cityId) {
        List<TheaterResponse> theaters = theaterRepository.findByCityIdAndIsActiveTrue(cityId)
                .stream()
                .map(t -> TheaterResponse.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .address(t.getAddress())
                        .cityName(t.getCity().getName())
                        .totalScreens(t.getTotalScreens())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(theaters));
    }
}
