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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transactsphere.account.dto.AccountUpdatedEvent;
import com.transactsphere.account.model.OutboxEvent;
import com.transactsphere.account.repository.OutboxEventRepository;
import java.time.LocalDateTime;
import org.springframework.retry.annotation.Retryable;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new account for a user.
     */
    @Transactional
    public AccountResponse createAccount(Long userId, AccountCreateRequest request) {
        if (accountRepository.existsByUserIdAndAccountType(userId, request.getAccountType())) {
            throw new com.transactsphere.account.exception.AccountAlreadyExistsException(
                    "User already has an account of type: " + request.getAccountType());
        }
        
        String accountNumber = generateUniqueAccountNumber();
        Account account = Account.builder()
                .accountNumber(accountNumber)
                .userId(userId)
                .accountType(request.getAccountType())
                .balance(BigDecimal.ZERO)
                .isFrozen(false)
                .build();

        Account saved = accountRepository.save(account);

        try {
            com.transactsphere.account.dto.GenericEvent event = com.transactsphere.account.dto.GenericEvent.builder()
                    .userId(userId)
                    .message("Your new account (" + accountNumber + ") of type " + request.getAccountType() + " has been created successfully.")
                    .build();
            kafkaTemplate.send("notification.generic", event);
        } catch (Exception e) {
            // Ignore kafka exceptions
        }

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
        publishCacheEvictEvent(accountNumber, "FREEZE_STATUS_CHANGE");
        
        try {
            com.transactsphere.account.dto.GenericEvent event = com.transactsphere.account.dto.GenericEvent.builder()
                    .userId(account.getUserId())
                    .message("Your account (" + accountNumber + ") has been " + (freeze ? "frozen" : "unfrozen") + ".")
                    .build();
            kafkaTemplate.send("notification.generic", event);
        } catch (Exception e) {
            // Ignore kafka exceptions
        }
        
        return mapToResponse(saved);
    }

    /**
     * Executes an internal transfer (debit, credit, or both).
     * Retries on lock acquisition failure.
     */
    @Retryable(
            retryFor = {CannotAcquireLockException.class, PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500)
    )
    @Transactional
    public void executeTransfer(InternalTransferRequest request) {
        BigDecimal amount = request.getAmount();

        String sourceNo = request.getSourceAccountNumber() != null && !request.getSourceAccountNumber().trim().isEmpty() ? request.getSourceAccountNumber().trim() : null;
        String targetNo = request.getTargetAccountNumber() != null && !request.getTargetAccountNumber().trim().isEmpty() ? request.getTargetAccountNumber().trim() : null;

        Account sourceAccount = null;
        Account targetAccount = null;

        // Prevent deadlocks by always locking accounts in a consistent order
        if (sourceNo != null && targetNo != null) {
            if (sourceNo.compareTo(targetNo) < 0) {
                sourceAccount = getAccountForUpdate(sourceNo, "Source");
                targetAccount = getAccountForUpdate(targetNo, "Target");
            } else if (sourceNo.compareTo(targetNo) > 0) {
                targetAccount = getAccountForUpdate(targetNo, "Target");
                sourceAccount = getAccountForUpdate(sourceNo, "Source");
            } else {
                sourceAccount = getAccountForUpdate(sourceNo, "Source");
                targetAccount = sourceAccount;
            }
        } else if (sourceNo != null) {
            sourceAccount = getAccountForUpdate(sourceNo, "Source");
        } else if (targetNo != null) {
            targetAccount = getAccountForUpdate(targetNo, "Target");
        }

        // 1. Debit operation
        if (sourceAccount != null) {
            if (sourceAccount.isFrozen()) {
                throw new AccountFrozenException("Source account is frozen: " + sourceNo);
            }
            if (sourceAccount.getBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException("Insufficient balance in source account: " + sourceNo);
            }
            sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
            accountRepository.save(sourceAccount);
            publishCacheEvictEvent(sourceNo, "BALANCE_UPDATE");
        }

        // 2. Credit operation
        if (targetAccount != null && targetAccount != sourceAccount) {
            if (targetAccount.isFrozen()) {
                throw new AccountFrozenException("Target account is frozen: " + targetNo);
            }
            targetAccount.setBalance(targetAccount.getBalance().add(amount));
            accountRepository.save(targetAccount);
            publishCacheEvictEvent(targetNo, "BALANCE_UPDATE");
        }
    }

    private Account getAccountForUpdate(String accountNumber, String type) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(type + " account not found: " + accountNumber));
    }

    /**
     * Saves an event to the outbox for background cache eviction.
     */
    private void publishCacheEvictEvent(String accountNumber, String action) {
        try {
            AccountUpdatedEvent payload = AccountUpdatedEvent.builder()
                    .accountNumber(accountNumber)
                    .action(action)
                    .timestamp(LocalDateTime.now())
                    .build();

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(accountNumber)
                    .eventType("account.updated")
                    .payload(objectMapper.writeValueAsString(payload))
                    .status("PENDING")
                    .build();
            
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save outbox event for cache eviction", e);
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

    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
}
