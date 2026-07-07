package com.transactsphere.fraud.controller;

import com.transactsphere.fraud.model.FraudLog;
import com.transactsphere.fraud.service.FraudService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
public class FraudController {

    private final FraudService fraudService;

    @GetMapping("/logs")
    public ResponseEntity<List<FraudLog>> getAllLogs(
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (!isAdminOrEmployee(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(fraudService.getAllFraudLogs());
    }

    @GetMapping("/logs/user/{userId}")
    public ResponseEntity<List<FraudLog>> getLogsByUser(
            @PathVariable("userId") Long userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (!isAdminOrEmployee(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(fraudService.getFraudLogsByUser(userId));
    }

    @PutMapping("/resolve/{id}")
    public ResponseEntity<FraudLog> resolveLog(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (!isAdminOrEmployee(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(fraudService.resolveFraudLog(id, username));
    }

    private boolean isAdminOrEmployee(String roles) {
        return roles != null && (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_EMPLOYEE"));
    }
}
