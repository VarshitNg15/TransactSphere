package com.transactsphere.account.dto;

import com.transactsphere.account.model.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountCreateRequest {
    @NotNull(message = "Account type is required (SAVINGS or CURRENT)")
    private AccountType accountType;
}
