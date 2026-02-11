package com.bookmyshow.movie.service;

import com.bookmyshow.movie.dto.ScreenResponse;
import com.bookmyshow.movie.dto.TheaterResponse;
import com.bookmyshow.movie.dto.admin.CreateScreenRequest;
import com.bookmyshow.movie.dto.admin.CreateTheaterRequest;
import com.bookmyshow.movie.model.City;
import com.bookmyshow.movie.model.Screen;
import com.bookmyshow.movie.model.Theater;
import com.bookmyshow.movie.repository.CityRepository;
import com.bookmyshow.movie.repository.ScreenRepository;
import com.bookmyshow.movie.repository.TheaterRepository;
import com.bookmyshow.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin service for theater and screen management.
 */
@Service
@RequiredArgsConstructor
public class AdminTheaterService {

    private static final Logger log = LoggerFactory.getLogger(AdminTheaterService.class);

    private final TheaterRepository theaterRepository;
    private final ScreenRepository screenRepository;
    private final CityRepository cityRepository;

    /**
     * Get all theaters.
     */
    public List<TheaterResponse> getAllTheaters() {
        return theaterRepository.findAll()
                .stream()
                .map(this::mapTheaterToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get theater by ID with screens.
     */
    public TheaterResponse getTheaterById(UUID theaterId) {
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new ResourceNotFoundException("Theater", "id", theaterId));
        return mapTheaterToResponse(theater);
    }

    /**
     * Create a new theater.
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "theaters", allEntries = true)
    })
    public TheaterResponse createTheater(CreateTheaterRequest request) {
        log.info("Creating new theater: {} in city {}", request.getName(), request.getCityId());
        
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City", "id", request.getCityId()));
        
        Theater theater = Theater.builder()
                .name(request.getName())
                .city(city)
                .address(request.getAddress())
                .phone(request.getPhone())
                .totalScreens(request.getTotalScreens() != null ? request.getTotalScreens() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        
        Theater saved = theaterRepository.save(theater);
        log.info("Created theater with ID: {}", saved.getId());
        
        return mapTheaterToResponse(saved);
    }

    /**
     * Update theater.
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "theaters", allEntries = true)
    })
    public TheaterResponse updateTheater(UUID theaterId, CreateTheaterRequest request) {
        log.info("Updating theater: {}", theaterId);
        
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new ResourceNotFoundException("Theater", "id", theaterId));
        
        if (request.getName() != null) theater.setName(request.getName());
        if (request.getAddress() != null) theater.setAddress(request.getAddress());
        if (request.getPhone() != null) theater.setPhone(request.getPhone());
        if (request.getTotalScreens() != null) theater.setTotalScreens(request.getTotalScreens());
        if (request.getIsActive() != null) theater.setIsActive(request.getIsActive());
        
        if (request.getCityId() != null) {
            City city = cityRepository.findById(request.getCityId())
                    .orElseThrow(() -> new ResourceNotFoundException("City", "id", request.getCityId()));
            theater.setCity(city);
        }
        
        Theater saved = theaterRepository.save(theater);
        log.info("Updated theater: {}", saved.getId());
        
        return mapTheaterToResponse(saved);
    }

    /**
     * Delete theater (soft delete).
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "theaters", allEntries = true)
    })
    public void deleteTheater(UUID theaterId) {
        log.info("Deleting theater: {}", theaterId);
        
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new ResourceNotFoundException("Theater", "id", theaterId));
        
        theater.setIsActive(false);
        theaterRepository.save(theater);
        
        log.info("Deactivated theater: {}", theaterId);
    }

    /**
     * Get screens for a theater.
     */
    public List<ScreenResponse> getScreensByTheater(UUID theaterId) {
        return screenRepository.findByTheaterId(theaterId)
                .stream()
                .map(this::mapScreenToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new screen.
     */
    @Transactional
    public ScreenResponse createScreen(CreateScreenRequest request) {
        log.info("Creating new screen: {} in theater {}", request.getName(), request.getTheaterId());
        
        Theater theater = theaterRepository.findById(request.getTheaterId())
                .orElseThrow(() -> new ResourceNotFoundException("Theater", "id", request.getTheaterId()));
        
        Screen screen = Screen.builder()
                .name(request.getName())
                .theater(theater)
                .totalSeats(request.getTotalSeats())
                .screenType(request.getScreenType())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        
        Screen saved = screenRepository.save(screen);
        
        // Update theater's total screens count
        theater.setTotalScreens(theater.getTotalScreens() + 1);
        theaterRepository.save(theater);
        
        log.info("Created screen with ID: {}", saved.getId());
        
        return mapScreenToResponse(saved);
    }

    /**
     * Update screen.
     */
    @Transactional
    public ScreenResponse updateScreen(UUID screenId, CreateScreenRequest request) {
        log.info("Updating screen: {}", screenId);
        
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", "id", screenId));
        
        if (request.getName() != null) screen.setName(request.getName());
        if (request.getTotalSeats() != null) screen.setTotalSeats(request.getTotalSeats());
        if (request.getScreenType() != null) screen.setScreenType(request.getScreenType());
        if (request.getIsActive() != null) screen.setIsActive(request.getIsActive());
        
        Screen saved = screenRepository.save(screen);
        log.info("Updated screen: {}", saved.getId());
        
        return mapScreenToResponse(saved);
    }

    /**
     * Delete screen (soft delete).
     */
    @Transactional
    public void deleteScreen(UUID screenId) {
        log.info("Deleting screen: {}", screenId);
        
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", "id", screenId));
        
        screen.setIsActive(false);
        screenRepository.save(screen);
        
        log.info("Deactivated screen: {}", screenId);
    }

    private TheaterResponse mapTheaterToResponse(Theater theater) {
        return TheaterResponse.builder()
                .id(theater.getId())
                .name(theater.getName())
                .address(theater.getAddress())
                .cityName(theater.getCity().getName())
                .totalScreens(theater.getTotalScreens())
                .build();
    }

    private ScreenResponse mapScreenToResponse(Screen screen) {
        return ScreenResponse.builder()
                .id(screen.getId())
                .name(screen.getName())
                .totalSeats(screen.getTotalSeats())
                .screenType(screen.getScreenType())
                .build();
    }
}
