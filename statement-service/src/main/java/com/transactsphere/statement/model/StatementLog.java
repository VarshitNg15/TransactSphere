package com.transactsphere.statement.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "statement_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatementLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String accountNumber;
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    private LocalDateTime generatedAt;
}
