package com.transactsphere.fraud.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionId;
    private String sourceAccountNumber;
    private String targetAccountNumber;
    private BigDecimal amount;
    private String transactionType;
    private String channel;
    private String status;
    private LocalDateTime timestamp;
    private String fraudReason;

    private boolean resolved;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
}
