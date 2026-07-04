package com.transactsphere.account.repository;

import com.transactsphere.account.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByUserId(Long userId);
    boolean existsByAccountNumber(String accountNumber);
    boolean existsByUserIdAndAccountType(Long userId, com.transactsphere.account.model.AccountType accountType);
}
