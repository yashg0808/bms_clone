package com.bookmyshow.movie.service;

import com.bookmyshow.movie.model.Seat;
import com.bookmyshow.movie.model.Screen;
import com.bookmyshow.movie.repository.ScreenRepository;
import com.bookmyshow.movie.repository.SeatRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * LayoutGenerator — Internal utility that exports static seat layouts as JSON files.
 *
 * Purpose: Decouple the seat LAYOUT (rows, numbers, types, columns — static data that
 *          never changes) from seat STATUS (AVAILABLE/LOCKED/BOOKED — dynamic data that
 *          changes on every booking). This reduces API payload by ~60% on subsequent
 *          seat-map loads since the layout can be cached by the browser/CDN indefinitely.
 *
 * Output:  /layouts/screen-{screenId}.json
 * Format:
 *   {
 *     "screenId": "uuid",
 *     "screenName": "Screen 1",
 *     "screenType": "IMAX",
 *     "totalSeats": 128,
 *     "sections": [
 *       {
 *         "type": "RECLINER",
 *         "rows": {
 *           "A": [
 *             { "seatId": "uuid", "number": "1", "column": 1 },
 *             ...
 *           ]
 *         }
 *       }
 *     ]
 *   }
 *
 * Schedule: Runs once at startup and then daily at 2 AM.
 *           Can also be triggered manually via the REST endpoint.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LayoutGenerator {

    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final ObjectMapper objectMapper;

    @Value("${layout.output-dir:./layouts}")
    private String outputDir;

    /**
     * Generate layout JSON for all active screens.
     * Runs at startup and daily at 2 AM.
     */
    @Scheduled(fixedDelay = Long.MAX_VALUE) // Run once at startup
    public void generateAllOnStartup() {
        generateAll();
    }

    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    public void generateAllDaily() {
        generateAll();
    }

    public int generateAll() {
        List<Screen> screens = screenRepository.findAll();
        int count = 0;
        for (Screen screen : screens) {
            if (screen.getIsActive()) {
                try {
                    generateForScreen(screen.getId());
                    count++;
                } catch (Exception e) {
                    log.error("Failed to generate layout for screen {}: {}", screen.getId(), e.getMessage());
                }
            }
        }
        log.info("Generated {} screen layouts to {}", count, outputDir);
        return count;
    }

    /**
     * Generate layout JSON for a single screen.
     */
    public void generateForScreen(UUID screenId) throws IOException {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new IllegalArgumentException("Screen not found: " + screenId));

        List<Seat> seats = seatRepository.findByScreenIdAndIsActiveTrueOrderByRowNameAscColumnNumberAsc(screenId);

        // Build the layout structure
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("screenId", screen.getId().toString());
        layout.put("screenName", screen.getName());
        layout.put("screenType", screen.getScreenType());
        layout.put("totalSeats", seats.size());

        // Group by seat type, preserving order: RECLINER → PREMIUM → REGULAR
        String[] typeOrder = {"RECLINER", "PREMIUM", "REGULAR", "VIP"};
        Map<String, List<Seat>> byType = seats.stream()
                .collect(Collectors.groupingBy(s -> s.getSeatType().name(), LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> sections = new ArrayList<>();
        for (String type : typeOrder) {
            List<Seat> typeSeats = byType.get(type);
            if (typeSeats == null || typeSeats.isEmpty()) continue;

            Map<String, Object> section = new LinkedHashMap<>();
            section.put("type", type);

            // Group by row within this type
            Map<String, List<Map<String, Object>>> rows = new LinkedHashMap<>();
            for (Seat seat : typeSeats) {
                rows.computeIfAbsent(seat.getRowName(), k -> new ArrayList<>())
                        .add(Map.of(
                                "seatId", seat.getId().toString(),
                                "number", seat.getSeatNumber(),
                                "column", seat.getColumnNumber()
                        ));
            }
            section.put("rows", rows);
            sections.add(section);
        }
        layout.put("sections", sections);

        // Write to file
        Path dir = Paths.get(outputDir);
        Files.createDirectories(dir);
        File file = dir.resolve("screen-" + screenId + ".json").toFile();

        objectMapper.writer()
                .withDefaultPrettyPrinter()
                .writeValue(file, layout);

        log.debug("Generated layout: {}", file.getAbsolutePath());
    }
}
