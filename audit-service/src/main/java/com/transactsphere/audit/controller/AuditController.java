package com.transactsphere.audit.controller;

import com.transactsphere.audit.model.AuditLog;
import com.transactsphere.audit.service.AuditService;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @PostMapping("/logs")
    public ResponseEntity<AuditLog> createAuditLog(
            @RequestBody AuditLogRequest request) {
        AuditLog logged = auditService.logCustomEvent(
                request.getEventType(),
                request.getMessage(),
                request.getUserId(),
                request.getServiceName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(logged);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getLogs(
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (!isAdminOrEmployee(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(auditService.getAuditLogs());
    }

    private boolean isAdminOrEmployee(String roles) {
        return roles != null && (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_EMPLOYEE"));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditLogRequest {
        private String eventType;
        private String message;
        private Long userId;
        private String serviceName;
    }
}
