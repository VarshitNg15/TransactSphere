package com.transactsphere.transaction.repository;

import com.transactsphere.transaction.model.MoneyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MoneyRequestRepository extends JpaRepository<MoneyRequest, Long> {
    List<MoneyRequest> findByRequesterAccountNumberOrderByCreatedAtDesc(String requesterAccountNumber);
    List<MoneyRequest> findByTargetUsernameOrderByCreatedAtDesc(String targetUsername);
}
