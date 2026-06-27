package com.transactsphere.transaction.dto;

import com.transactsphere.transaction.model.TransactionChannel;
import com.transactsphere.transaction.model.TransactionStatus;
import com.transactsphere.transaction.model.TransactionType;
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
    private TransactionType transactionType;
    private TransactionChannel channel;
    private TransactionStatus status;
    private String description;
    private Long userId;
    private LocalDateTime timestamp;
}
