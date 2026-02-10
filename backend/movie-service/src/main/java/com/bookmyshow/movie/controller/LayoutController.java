package com.bookmyshow.movie.controller;

import com.bookmyshow.movie.service.LayoutGenerator;
import com.bookmyshow.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Internal endpoint to trigger layout generation.
 * In production, this would be an admin-only endpoint.
 */
@RestController
@RequestMapping("/api/v1/layouts")
@RequiredArgsConstructor
public class LayoutController {

    private final LayoutGenerator layoutGenerator;

    /**
     * Regenerate all screen layouts.
     * POST /api/v1/layouts/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateAll() {
        int count = layoutGenerator.generateAll();
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("screensGenerated", count),
                "Layout generation complete"
        ));
    }

    /**
     * Regenerate layout for a specific screen.
     * POST /api/v1/layouts/generate/{screenId}
     */
    @PostMapping("/generate/{screenId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateForScreen(@PathVariable UUID screenId) {
        try {
            layoutGenerator.generateForScreen(screenId);
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("screenId", screenId),
                    "Layout generated for screen"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(e.getMessage(), "LAYOUT_GENERATION_FAILED"));
        }
    }
}
