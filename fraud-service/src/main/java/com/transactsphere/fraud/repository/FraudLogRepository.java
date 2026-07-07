package com.transactsphere.fraud.repository;

import com.transactsphere.fraud.model.FraudLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudLogRepository extends JpaRepository<FraudLog, Long> {
    List<FraudLog> findBySourceAccountNumberInOrTargetAccountNumberInOrderByTimestampDesc(
            List<String> sourceAccounts, List<String> targetAccounts);
}
