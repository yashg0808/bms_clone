package com.bookmyshow.movie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Movie Service Application - handles movies, theaters, screens, and shows.
 */
@SpringBootApplication(scanBasePackages = {"com.bookmyshow.movie", "com.bookmyshow.shared"})
@EnableJpaAuditing
@EnableCaching
@EnableScheduling
public class MovieServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieServiceApplication.class, args);
    }
}
