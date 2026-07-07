package com.transactsphere.analytics.controller;

import com.transactsphere.analytics.model.UserAnalytics;
import com.transactsphere.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (!isAdminOrEmployee(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(analyticsService.getDashboardStats());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<UserAnalytics> getUserStats(
            @PathVariable("userId") Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (!userId.equals(headerUserId) && !isAdminOrEmployee(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(analyticsService.getUserAnalytics(userId));
    }

    private boolean isAdminOrEmployee(String roles) {
        return roles != null && (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_EMPLOYEE"));
    }
}
