package com.transactsphere.statement.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private Long id;
    private String transactionId;
    private String sourceAccountNumber;
    private String targetAccountNumber;
    private BigDecimal amount;
    private String transactionType;
    private String channel;
    private String status;
    private String description;
    private Long userId;
    private LocalDateTime timestamp;
}
