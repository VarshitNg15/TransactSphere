package com.transactsphere.transaction.dto;

import com.transactsphere.transaction.model.MoneyRequestStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoneyRequestDto {
    private Long id;
    private String requesterAccountNumber;
    private String targetUsername;
    private BigDecimal amount;
    private String description;
    private MoneyRequestStatus status;
    private LocalDateTime createdAt;
}
