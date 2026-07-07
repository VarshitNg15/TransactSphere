package com.transactsphere.admin.controller;

import com.transactsphere.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (!isAdminOrEmployee(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(adminService.getPlatformStats(roles));
    }

    @PutMapping("/users/{userId}/kyc")
    public ResponseEntity<Object> updateKyc(
            @PathVariable("userId") Long userId,
            @RequestParam("status") String status,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (!isAdminOrEmployee(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Object response = adminService.updateKycStatus(userId, status, roles);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/accounts/{accountNumber}/freeze")
    public ResponseEntity<Object> freezeAccount(
            @PathVariable("accountNumber") String accountNumber,
            @RequestParam("freeze") boolean freeze,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (!isAdminOrEmployee(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Object response = adminService.setAccountFreezeStatus(accountNumber, freeze, roles);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fraud/logs")
    public ResponseEntity<List<Object>> getFraudLogs(
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (!isAdminOrEmployee(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(adminService.getFraudLogs(roles));
    }

    @PutMapping("/fraud/resolve/{id}")
    public ResponseEntity<Object> resolveFraud(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (!isAdminOrEmployee(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Object response = adminService.resolveFraudIncident(id, username, roles);
        return ResponseEntity.ok(response);
    }

    private boolean isAdminOrEmployee(String roles) {
        return roles != null && (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_EMPLOYEE"));
    }
}
