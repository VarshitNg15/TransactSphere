package com.transactsphere.transaction.model;

import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateId; // e.g. transactionId

    @Column(nullable = false)
    private String eventType; // e.g. topic name

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload; // JSON payload

    @Column(nullable = false)
    private String status; // PENDING, SENT

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
