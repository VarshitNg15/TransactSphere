package com.transactsphere.account.dto;

import com.transactsphere.account.model.AccountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {
    private Long id;
    private String accountNumber;
    private Long userId;
    private AccountType accountType;
    private BigDecimal balance;
    private boolean isFrozen;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
