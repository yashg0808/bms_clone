package com.bookmyshow.movie.service;

import com.bookmyshow.movie.config.CacheEventLogger;
import com.bookmyshow.movie.dto.CityResponse;
import com.bookmyshow.movie.dto.TheaterResponse;
import com.bookmyshow.movie.repository.CityRepository;
import com.bookmyshow.movie.repository.ScreenRepository;
import com.bookmyshow.movie.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for theater and city operations.
 * All methods are cached since this is mostly static reference data.
 */
@Service
@RequiredArgsConstructor
public class TheaterService {

    private static final Logger log = LoggerFactory.getLogger(TheaterService.class);

    private final TheaterRepository theaterRepository;
    private final CityRepository cityRepository;
    private final ScreenRepository screenRepository;

    /**
     * Get all active cities.
     * Cached for 24 hours — cities rarely change.
     */
    @Cacheable(value = "cities", key = "'all'")
    public List<CityResponse> getAllCities() {
        CacheEventLogger.logCacheMiss("cities", "all");
        return cityRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(c -> CityResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .state(c.getState())
                        .country(c.getCountry())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get theaters in a city.
     * Cached for 6 hours per city.
     */
    @Cacheable(value = "theaters", key = "#cityId")
    public List<TheaterResponse> getTheatersByCity(UUID cityId) {
        CacheEventLogger.logCacheMiss("theaters", cityId);
        return theaterRepository.findByCityIdAndIsActiveTrue(cityId)
                .stream()
                .map(t -> TheaterResponse.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .address(t.getAddress())
                        .cityName(t.getCity().getName())
                        .totalScreens((int) screenRepository.countByTheaterIdAndIsActiveTrue(t.getId()))
                        .build())
                .collect(Collectors.toList());
    }
}
