package com.transactsphere.account.service;

import com.transactsphere.account.dto.AccountCreateRequest;
import com.transactsphere.account.dto.AccountResponse;
import com.transactsphere.account.dto.InternalTransferRequest;
import com.transactsphere.account.exception.AccountFrozenException;
import com.transactsphere.account.exception.AccountNotFoundException;
import com.transactsphere.account.exception.InsufficientBalanceException;
import com.transactsphere.account.model.Account;
import com.transactsphere.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final CacheManager cacheManager;

    /**
     * Creates a new account for a user.
     */
    @Transactional
    public AccountResponse createAccount(Long userId, AccountCreateRequest request) {
        String accountNumber = generateUniqueAccountNumber();
        Account account = Account.builder()
                .accountNumber(accountNumber)
                .userId(userId)
                .accountType(request.getAccountType())
                .balance(BigDecimal.ZERO)
                .isFrozen(false)
                .build();

        Account saved = accountRepository.save(account);
        return mapToResponse(saved);
    }

    /**
     * Gets all accounts belonging to a user.
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gets details of an account. Caches the result in Redis.
     */
    @Cacheable(value = "accounts", key = "#accountNumber")
    @Transactional(readOnly = true)
    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        return mapToResponse(account);
    }

    /**
     * Freezes or unfreezes an account (restricted to admin/employee).
     */
    @Transactional
    public AccountResponse setFreezeStatus(String accountNumber, boolean freeze) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        account.setFrozen(freeze);
        Account saved = accountRepository.save(account);
        
        // Evict from cache
        evictCache(accountNumber);
        
        return mapToResponse(saved);
    }

    /**
     * Executes an internal transfer (debit, credit, or both).
     */
    @Transactional
    public void executeTransfer(InternalTransferRequest request) {
        BigDecimal amount = request.getAmount();

        // 1. Debit operation (if sourceAccountNumber is present)
        if (request.getSourceAccountNumber() != null && !request.getSourceAccountNumber().trim().isEmpty()) {
            String sourceNo = request.getSourceAccountNumber().trim();
            Account sourceAccount = accountRepository.findByAccountNumber(sourceNo)
                    .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + sourceNo));

            if (sourceAccount.isFrozen()) {
                throw new AccountFrozenException("Source account is frozen: " + sourceNo);
            }

            if (sourceAccount.getBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException("Insufficient balance in source account: " + sourceNo);
            }

            sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
            accountRepository.save(sourceAccount);
            evictCache(sourceNo);
        }

        // 2. Credit operation (if targetAccountNumber is present)
        if (request.getTargetAccountNumber() != null && !request.getTargetAccountNumber().trim().isEmpty()) {
            String targetNo = request.getTargetAccountNumber().trim();
            Account targetAccount = accountRepository.findByAccountNumber(targetNo)
                    .orElseThrow(() -> new AccountNotFoundException("Target account not found: " + targetNo));

            if (targetAccount.isFrozen()) {
                throw new AccountFrozenException("Target account is frozen: " + targetNo);
            }

            targetAccount.setBalance(targetAccount.getBalance().add(amount));
            accountRepository.save(targetAccount);
            evictCache(targetNo);
        }
    }

    /**
     * Evicts an account from Redis cache programmatically.
     */
    private void evictCache(String accountNumber) {
        if (cacheManager.getCache("accounts") != null) {
            cacheManager.getCache("accounts").evict(accountNumber);
        }
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            long number = (long) (Math.random() * 90000000L) + 10000000L;
            accountNumber = "1000" + number;
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .userId(account.getUserId())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .isFrozen(account.isFrozen())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
