package com.transactsphere.statement.repository;

import com.transactsphere.statement.model.StatementLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatementLogRepository extends JpaRepository<StatementLog, Long> {
    List<StatementLog> findByUserIdOrderByGeneratedAtDesc(Long userId);
}
