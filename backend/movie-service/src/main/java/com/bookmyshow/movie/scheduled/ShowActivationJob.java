package com.bookmyshow.movie.scheduled;

import com.bookmyshow.movie.model.Show;
import com.bookmyshow.movie.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled job to deactivate past shows.
 */
@Component
@RequiredArgsConstructor
public class ShowActivationJob {

    private static final Logger log = LoggerFactory.getLogger(ShowActivationJob.class);

    private final ShowRepository showRepository;

    /**
     * Runs daily at midnight to deactivate shows with past dates.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deactivatePastShows() {
        log.info("Running ShowActivationJob: deactivating past shows...");
        List<Show> pastShows = showRepository.findPastActiveShows(LocalDate.now());
        pastShows.forEach(show -> show.setIsActive(false));
        showRepository.saveAll(pastShows);
        log.info("Deactivated {} past shows", pastShows.size());
    }
}
