package com.bookmyshow.movie.service;

import com.bookmyshow.movie.dto.ShowResponse;
import com.bookmyshow.movie.dto.PagedResponse;
import com.bookmyshow.movie.dto.admin.CreateShowRequest;
import com.bookmyshow.movie.dto.admin.UpdateShowRequest;
import com.bookmyshow.movie.model.Movie;
import com.bookmyshow.movie.model.Screen;
import com.bookmyshow.movie.model.Show;
import com.bookmyshow.movie.repository.MovieRepository;
import com.bookmyshow.movie.repository.ScreenRepository;
import com.bookmyshow.movie.repository.ShowRepository;
import com.bookmyshow.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin service for show scheduling operations.
 */
@Service
@RequiredArgsConstructor
public class AdminShowService {

    private static final Logger log = LoggerFactory.getLogger(AdminShowService.class);

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;

    /**
     * Get all shows with filtering options.
     */
    public PagedResponse<ShowResponse> getAllShows(int page, int size, UUID movieId, UUID screenId, LocalDate date) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("showDate", "startTime").descending());
        
        Page<Show> shows;
        if (movieId != null && date != null) {
            shows = showRepository.findByMovieIdAndShowDate(movieId, date, pageable);
        } else if (movieId != null) {
            shows = showRepository.findByMovieId(movieId, pageable);
        } else if (screenId != null && date != null) {
            shows = showRepository.findByScreenIdAndShowDate(screenId, date, pageable);
        } else if (date != null) {
            shows = showRepository.findByShowDate(date, pageable);
        } else {
            shows = showRepository.findAll(pageable);
        }
        
        Page<ShowResponse> pageResult = shows.map(this::mapToResponse);
        return toPagedResponse(pageResult);
    }

    /**
     * Get shows for a specific screen on a date.
     */
    public List<ShowResponse> getShowsByScreenAndDate(UUID screenId, LocalDate date) {
        return showRepository.findByScreenIdAndShowDateOrderByStartTime(screenId, date)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new show.
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "shows-by-movie", allEntries = true)
    })
    public ShowResponse createShow(CreateShowRequest request) {
        log.info("Creating new show for movie {} on screen {} at {}", 
                request.getMovieId(), request.getScreenId(), request.getShowDate());
        
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie", "id", request.getMovieId()));
        
        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen", "id", request.getScreenId()));
        
        // Check for time conflicts on same screen
        List<Show> conflictingShows = showRepository.findConflictingShows(
                request.getScreenId(), 
                request.getShowDate(), 
                request.getStartTime(), 
                request.getEndTime());
        
        if (!conflictingShows.isEmpty()) {
            throw new IllegalArgumentException("Time conflict with existing show(s) on this screen");
        }
        
        Show show = Show.builder()
                .movie(movie)
                .screen(screen)
                .showDate(request.getShowDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .basePrice(request.getBasePrice())
                .premiumPrice(request.getPremiumPrice())
                .reclinerPrice(request.getReclinerPrice())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        
        Show saved = showRepository.save(show);
        log.info("Created show with ID: {}", saved.getId());
        
        return mapToResponse(saved);
    }

    /**
     * Update an existing show.
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "shows", key = "#showId"),
        @CacheEvict(value = "shows-by-movie", allEntries = true)
    })
    public ShowResponse updateShow(UUID showId, UpdateShowRequest request) {
        log.info("Updating show: {}", showId);
        
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", "id", showId));
        
        if (request.getMovieId() != null) {
            Movie movie = movieRepository.findById(request.getMovieId())
                    .orElseThrow(() -> new ResourceNotFoundException("Movie", "id", request.getMovieId()));
            show.setMovie(movie);
        }
        
        if (request.getScreenId() != null) {
            Screen screen = screenRepository.findById(request.getScreenId())
                    .orElseThrow(() -> new ResourceNotFoundException("Screen", "id", request.getScreenId()));
            show.setScreen(screen);
        }
        
        if (request.getShowDate() != null) show.setShowDate(request.getShowDate());
        if (request.getStartTime() != null) show.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) show.setEndTime(request.getEndTime());
        if (request.getBasePrice() != null) show.setBasePrice(request.getBasePrice());
        if (request.getPremiumPrice() != null) show.setPremiumPrice(request.getPremiumPrice());
        if (request.getReclinerPrice() != null) show.setReclinerPrice(request.getReclinerPrice());
        if (request.getIsActive() != null) show.setIsActive(request.getIsActive());
        
        Show saved = showRepository.save(show);
        log.info("Updated show: {}", saved.getId());
        
        return mapToResponse(saved);
    }

    /**
     * Delete a show (soft delete).
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "shows", key = "#showId"),
        @CacheEvict(value = "shows-by-movie", allEntries = true)
    })
    public void deleteShow(UUID showId) {
        log.info("Deleting (deactivating) show: {}", showId);
        
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", "id", showId));
        
        show.setIsActive(false);
        showRepository.save(show);
        
        log.info("Deactivated show: {}", showId);
    }

    /**
     * Bulk create shows for a movie across multiple screens.
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "shows-by-movie", allEntries = true)
    })
    public List<ShowResponse> bulkCreateShows(List<CreateShowRequest> requests) {
        log.info("Bulk creating {} shows", requests.size());
        
        return requests.stream()
                .map(this::createShow)
                .collect(Collectors.toList());
    }

    private <T> PagedResponse<T> toPagedResponse(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private ShowResponse mapToResponse(Show show) {
        return ShowResponse.builder()
                .id(show.getId())
                .movieId(show.getMovie().getId())
                .movieTitle(show.getMovie().getTitle())
                .screenId(show.getScreen().getId())
                .screenName(show.getScreen().getName())
                .screenType(show.getScreen().getScreenType())
                .theaterName(show.getScreen().getTheater().getName())
                .theaterId(show.getScreen().getTheater().getId())
                .showDate(show.getShowDate())
                .startTime(show.getStartTime())
                .endTime(show.getEndTime())
                .basePrice(show.getBasePrice())
                .premiumPrice(show.getPremiumPrice())
                .reclinerPrice(show.getReclinerPrice())
                .build();
    }
}
