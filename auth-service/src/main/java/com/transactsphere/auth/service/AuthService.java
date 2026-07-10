package com.transactsphere.auth.service;

import com.transactsphere.auth.dto.*;
import com.transactsphere.auth.exception.InvalidTokenException;
import com.transactsphere.auth.exception.UserAlreadyExistsException;
import com.transactsphere.auth.model.User;
import com.transactsphere.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Registers a new user. Throws exception if username or email is already taken.
     */
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email is already taken");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        userRepository.save(user);
    }

    /**
     * Authenticates credentials, generates JWT access and refresh tokens.
     */
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.getUsername()));

        String accessToken = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole().name(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Refreshes access token using a valid, non-expired refresh token.
     */
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        
        try {
            if (jwtService.isTokenExpired(refreshToken)) {
                throw new InvalidTokenException("Refresh token has expired");
            }
            
            String username = jwtService.extractUsername(refreshToken);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
            
            if (!jwtService.isTokenValid(refreshToken, user.getUsername())) {
                throw new InvalidTokenException("Refresh token is invalid");
            }

            String newAccessToken = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole().name(), user.getEmail());
            // Optionally, we could rotate refresh token here as well. For now, we reuse the existing one.
            return AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .build();

        } catch (Exception e) {
            throw new InvalidTokenException("Invalid refresh token: " + e.getMessage());
        }
    }

    /**
     * Blacklists an access token to terminate session instantly.
     */
    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtService.blacklistToken(token);
        }
    }

    /**
     * Resets user password directly if username and email match.
     */
    @Transactional
    public void resetPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.getEmail().equalsIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("Email does not match the registered email for this user");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void blockUser(Long userId, boolean block) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setActive(!block);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
