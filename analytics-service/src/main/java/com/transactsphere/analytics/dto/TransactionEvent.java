package com.transactsphere.analytics.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEvent {
    private String transactionId;
    private String sourceAccountId;
    private String targetAccountId;
    private BigDecimal amount;
    private String transactionType;
    private String channel;
    private String status;
    private LocalDateTime timestamp;
    private String fraudReason;
}
