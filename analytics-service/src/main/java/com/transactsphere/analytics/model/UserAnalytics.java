package com.transactsphere.analytics.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_analytics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAnalytics {

    @Id
    private Long userId;

    private BigDecimal totalVolume;
    private Long totalCount;
    
    private BigDecimal depositVolume;
    private BigDecimal withdrawalVolume;
    private BigDecimal transferVolume;

    private LocalDateTime lastTransactionTimestamp;
}
