package com.transactsphere.user.controller;

import com.transactsphere.user.dto.UserProfileResponse;
import com.transactsphere.user.dto.UserProfileUpdateRequest;
import com.transactsphere.user.model.KycStatus;
import com.transactsphere.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Name") String username) {
        UserProfileResponse response = userProfileService.getOrCreateProfile(userId, username);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Name") String username,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        UserProfileResponse response = userProfileService.updateProfile(userId, username, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<UserProfileResponse>> getAllProfiles(
            @RequestHeader("X-User-Roles") String roles) {
        if (!isAdminOrEmployee(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(userProfileService.getAllProfiles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getProfileById(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Roles") String roles) {
        if (!isAdminOrEmployee(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(userProfileService.getProfileById(id));
    }

    @PutMapping("/{id}/kyc")
    public ResponseEntity<UserProfileResponse> updateKyc(
            @PathVariable("id") Long id,
            @RequestParam("status") KycStatus status,
            @RequestHeader("X-User-Roles") String roles) {
        if (!isAdminOrEmployee(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        UserProfileResponse response = userProfileService.updateKycStatus(id, status);
        return ResponseEntity.ok(response);
    }

    private boolean isAdminOrEmployee(String roles) {
        return roles != null && (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_EMPLOYEE"));
    }
}
