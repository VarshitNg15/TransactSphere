package com.transactsphere.audit.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType;
    
    @Column(length = 1000)
    private String message;
    
    private Long userId;
    private LocalDateTime timestamp;
    private String serviceName;
}
