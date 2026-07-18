package com.transactsphere.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountUpdatedEvent {
    private String accountNumber;
    private String action;
    private LocalDateTime timestamp;
}
