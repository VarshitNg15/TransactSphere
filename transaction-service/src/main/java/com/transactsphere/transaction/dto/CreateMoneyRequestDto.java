package com.transactsphere.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateMoneyRequestDto {
    @NotBlank
    private String requesterAccountNumber;
    
    @NotBlank
    private String targetUsername;
    
    @NotNull
    @DecimalMin(value = "1.00")
    private BigDecimal amount;
    
    private String description;
}
