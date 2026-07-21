package com.transactsphere.auth.service;

import com.transactsphere.auth.dto.*;
import com.transactsphere.auth.exception.InvalidTokenException;
import com.transactsphere.auth.exception.UserAlreadyExistsException;
import com.transactsphere.auth.model.Role;
import com.transactsphere.auth.model.User;
import com.transactsphere.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

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
                .username("john_doe")
                .email("john@example.com")
                .password("password123")
                .role(Role.CUSTOMER)
                .build();

        loginRequest = LoginRequest.builder()
                .username("john_doe")
                .password("password123")
                .build();

        testUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .password("encoded_password")
                .role(Role.CUSTOMER)
                .isActive(true)
                .tokenVersion(1)
                .build();
    }

    @Test
    void register_Success() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertDoesNotThrow(() -> authService.register(registerRequest));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_ThrowsException_WhenUsernameExists() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class,
                () -> authService.register(registerRequest));

        assertEquals("Username is already taken", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser.getId(), testUser.getUsername(), testUser.getRole().name(), testUser.getEmail(), testUser.getTokenVersion()))
                .thenReturn("access_token");
        when(jwtService.generateRefreshToken(testUser.getUsername(), testUser.getTokenVersion())).thenReturn("refresh_token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals(testUser.getId(), response.getUserId());
        assertEquals(testUser.getUsername(), response.getUsername());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_ThrowsException_WhenCredentialsInvalid() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void refreshToken_Success() {
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest("valid_refresh_token");
        when(jwtService.isTokenExpired(refreshRequest.getRefreshToken())).thenReturn(false);
        when(jwtService.extractUsername(refreshRequest.getRefreshToken())).thenReturn("john_doe");
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenValid(refreshRequest.getRefreshToken(), testUser.getUsername())).thenReturn(true);
        when(jwtService.extractTokenVersion(refreshRequest.getRefreshToken())).thenReturn(1);
        when(jwtService.generateToken(testUser.getId(), testUser.getUsername(), testUser.getRole().name(), testUser.getEmail(), testUser.getTokenVersion()))
                .thenReturn("new_access_token");

        AuthResponse response = authService.refreshToken(refreshRequest);

        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        assertEquals("valid_refresh_token", response.getRefreshToken());
    }

    @Test
    void refreshToken_ThrowsException_WhenTokenExpired() {
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest("expired_refresh_token");
        when(jwtService.isTokenExpired(refreshRequest.getRefreshToken())).thenReturn(true);

        InvalidTokenException exception = assertThrows(InvalidTokenException.class,
                () -> authService.refreshToken(refreshRequest));

        assertTrue(exception.getMessage().contains("Refresh token has expired"));
    }
}
