package com.transactsphere.transaction.repository;

import com.transactsphere.transaction.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionId(String transactionId);
    List<Transaction> findBySourceAccountNumberInOrTargetAccountNumberInOrderByTimestampDesc(
            List<String> sourceAccounts, List<String> targetAccounts);
}
