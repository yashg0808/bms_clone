package com.bookmyshow.movie.service;

import com.bookmyshow.movie.dto.ShowResponse;
import com.bookmyshow.movie.model.Show;
import com.bookmyshow.movie.repository.ShowRepository;
import com.bookmyshow.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for show scheduling operations.
 */
@Service
@RequiredArgsConstructor
public class ShowService {

    private static final Logger log = LoggerFactory.getLogger(ShowService.class);

    private final ShowRepository showRepository;

    /**
     * Get shows for a movie on a specific date.
     */
    public List<ShowResponse> getShowsByMovieAndDate(UUID movieId, LocalDate date) {
        return showRepository.findByMovieIdAndShowDate(movieId, date)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get shows for a movie in a specific city on a date.
     */
    public List<ShowResponse> getShowsByMovieCityAndDate(UUID movieId, UUID cityId, LocalDate date) {
        return showRepository.findByMovieCityAndDate(movieId, cityId, date)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a show by ID.
     */
    public ShowResponse getShowById(UUID showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", "id", showId));
        return mapToResponse(show);
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
