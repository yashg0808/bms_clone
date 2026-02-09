package com.bookmyshow.user.service;

import com.bookmyshow.user.dto.LoginRequest;
import com.bookmyshow.user.dto.RegisterRequest;
import com.bookmyshow.user.dto.AuthResponse;
import com.bookmyshow.user.model.User;
import com.bookmyshow.user.model.UserRole;
import com.bookmyshow.user.repository.UserRepository;
import com.bookmyshow.user.security.JwtTokenProvider;
import com.bookmyshow.shared.exception.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .email("test@example.com")
                .password("Test@1234")
                .firstName("John")
                .lastName("Doe")
                .phone("+919876543210")
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("Test@1234")
                .build();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("$2a$12$encoded")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.CUSTOMER)
                .build();
    }

    @Test
    @DisplayName("Should register user successfully")
    void register_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$encoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateToken(anyString())).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("test@example.com", response.getUser().getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email exists")
    void register_DuplicateEmail() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () ->
                authService.register(registerRequest));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should login user successfully")
    void login_Success() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("John", response.getUser().getFirstName());
    }
}
