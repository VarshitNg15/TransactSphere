package com.transactsphere.auth.controller;

import com.transactsphere.auth.dto.*;
import com.transactsphere.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User registered successfully");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        authService.logout(authHeader);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.resetPassword(request);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Password reset successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{id}/block")
    public ResponseEntity<Map<String, String>> blockUser(
            @PathVariable Long id, 
            @RequestParam("block") boolean block, 
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (roles == null || !roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        authService.blockUser(id, block);
        Map<String, String> response = new HashMap<>();
        response.put("message", block ? "User blocked successfully" : "User unblocked successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers(
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (roles == null || !roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<Map<String, Object>> userList = new java.util.ArrayList<>();
        for (com.transactsphere.auth.model.User u : authService.getAllUsers()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("isBlocked", !u.isActive());
            userList.add(map);
        }
        return ResponseEntity.ok(userList);
    }
}
