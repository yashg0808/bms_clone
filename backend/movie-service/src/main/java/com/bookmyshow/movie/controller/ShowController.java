package com.bookmyshow.movie.controller;

import com.bookmyshow.movie.dto.ShowResponse;
import com.bookmyshow.movie.service.ShowService;
import com.bookmyshow.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for show operations.
 */
@RestController
@RequestMapping("/api/v1/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    /**
     * Get show details by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowResponse>> getShowById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(showService.getShowById(id)));
    }
}
