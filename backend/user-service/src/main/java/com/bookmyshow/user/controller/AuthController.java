package com.bookmyshow.user.controller;

import com.bookmyshow.shared.dto.ApiResponse;
import com.bookmyshow.user.dto.AuthResponse;
import com.bookmyshow.user.dto.LoginRequest;
import com.bookmyshow.user.dto.RegisterRequest;
import com.bookmyshow.user.dto.UserResponse;
import com.bookmyshow.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user.
     *
     * @param request registration details
     * @return JWT token and user info
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User registered successfully"));
    }

    /**
     * Authenticate a user.
     *
     * @param request login credentials
     * @return JWT token and user info
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    /**
     * Validate a JWT token.
     *
     * @param token JWT token from Authorization header
     * @return user info if token is valid
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<UserResponse>> validate(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        UserResponse user = authService.validateToken(token);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
