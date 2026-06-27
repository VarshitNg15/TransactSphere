package com.transactsphere.transaction.service;

import com.transactsphere.transaction.client.AccountClient;
import com.transactsphere.transaction.dto.*;
import com.transactsphere.transaction.model.*;
import com.transactsphere.transaction.repository.TransactionRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final BigDecimal MAX_LIMIT = new BigDecimal("200000.00");
    private static final BigDecimal MIN_LIMIT = new BigDecimal("1.00");
    private static final String TOPIC_TRANSACTION_COMPLETED = "transaction.completed";

    /**
     * Executes a deposit into a target account.
     */
    @Transactional
    public TransactionResponse deposit(Long userId, DepositRequest request) {
        // Validate target account existence and freeze status
        AccountDto targetAccount = getAndValidateAccount(request.getTargetAccountNumber());

        String txId = "TXN-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase();
        Transaction transaction = Transaction.builder()
                .transactionId(txId)
                .targetAccountNumber(request.getTargetAccountNumber())
                .amount(request.getAmount())
                .transactionType(TransactionType.DEPOSIT)
                .channel(TransactionChannel.INTERNAL)
                .status(TransactionStatus.PENDING)
                .description(request.getDescription())
                .userId(userId)
                .build();

        transaction = transactionRepository.save(transaction);

        try {
            // Execute internal balance credit
            InternalTransferRequest transferReq = InternalTransferRequest.builder()
                    .targetAccountNumber(request.getTargetAccountNumber())
                    .amount(request.getAmount())
                    .build();
            accountClient.transferInternal(transferReq);

            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction = transactionRepository.save(transaction);

            // Publish completed event to Kafka
            publishCompletedEvent(transaction);

            return mapToResponse(transaction);
        } catch (Exception e) {
            log.error("Failed to complete deposit. Error: {}", e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setDescription(request.getDescription() + " (Failed: " + extractErrorMessage(e) + ")");
            transactionRepository.save(transaction);
            throw new RuntimeException("Deposit execution failed: " + extractErrorMessage(e), e);
        }
    }

    /**
     * Executes a withdrawal from a source account.
     */
    @Transactional
    public TransactionResponse withdraw(Long userId, String roles, WithdrawRequest request) {
        // Validate source account
        AccountDto sourceAccount = getAndValidateAccount(request.getSourceAccountNumber());

        // Security check: Customer can only withdraw from their own account
        if (!sourceAccount.getUserId().equals(userId) && !isAdminOrEmployee(roles)) {
            throw new IllegalArgumentException("You do not own the source account: " + request.getSourceAccountNumber());
        }

        String txId = "TXN-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase();
        Transaction transaction = Transaction.builder()
                .transactionId(txId)
                .sourceAccountNumber(request.getSourceAccountNumber())
                .amount(request.getAmount())
                .transactionType(TransactionType.WITHDRAWAL)
                .channel(TransactionChannel.INTERNAL)
                .status(TransactionStatus.PENDING)
                .description(request.getDescription())
                .userId(userId)
                .build();

        transaction = transactionRepository.save(transaction);

        try {
            // Execute internal balance debit
            InternalTransferRequest transferReq = InternalTransferRequest.builder()
                    .sourceAccountNumber(request.getSourceAccountNumber())
                    .amount(request.getAmount())
                    .build();
            accountClient.transferInternal(transferReq);

            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction = transactionRepository.save(transaction);

            // Publish completed event to Kafka
            publishCompletedEvent(transaction);

            return mapToResponse(transaction);
        } catch (Exception e) {
            log.error("Failed to complete withdrawal. Error: {}", e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setDescription(request.getDescription() + " (Failed: " + extractErrorMessage(e) + ")");
            transactionRepository.save(transaction);
            throw new RuntimeException("Withdrawal execution failed: " + extractErrorMessage(e), e);
        }
    }

    /**
     * Executes a fund transfer from a source to a target account.
     */
    @Transactional
    public TransactionResponse transfer(Long userId, String roles, TransferRequest request) {
        BigDecimal amount = request.getAmount();

        // Validate limits
        if (amount.compareTo(MIN_LIMIT) < 0 || amount.compareTo(MAX_LIMIT) > 0) {
            throw new IllegalArgumentException("Transaction amount must be between ₹" + MIN_LIMIT + " and ₹" + MAX_LIMIT);
        }

        // Validate accounts
        AccountDto sourceAccount = getAndValidateAccount(request.getSourceAccountNumber());
        AccountDto targetAccount = getAndValidateAccount(request.getTargetAccountNumber());

        // Security check: Customer can only transfer from their own account
        if (!sourceAccount.getUserId().equals(userId) && !isAdminOrEmployee(roles)) {
            throw new IllegalArgumentException("You do not own the source account: " + request.getSourceAccountNumber());
        }

        String txId = "TXN-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase();
        Transaction transaction = Transaction.builder()
                .transactionId(txId)
                .sourceAccountNumber(request.getSourceAccountNumber())
                .targetAccountNumber(request.getTargetAccountNumber())
                .amount(amount)
                .transactionType(TransactionType.TRANSFER)
                .channel(request.getChannel())
                .status(TransactionStatus.PENDING)
                .description(request.getDescription())
                .userId(userId)
                .build();

        transaction = transactionRepository.save(transaction);

        try {
            // Execute internal balance updates
            InternalTransferRequest transferReq = InternalTransferRequest.builder()
                    .sourceAccountNumber(request.getSourceAccountNumber())
                    .targetAccountNumber(request.getTargetAccountNumber())
                    .amount(amount)
                    .build();
            accountClient.transferInternal(transferReq);

            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction = transactionRepository.save(transaction);

            // Publish completed event to Kafka
            publishCompletedEvent(transaction);

            return mapToResponse(transaction);
        } catch (Exception e) {
            log.error("Failed to complete transfer. Error: {}", e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setDescription(request.getDescription() + " (Failed: " + extractErrorMessage(e) + ")");
            transactionRepository.save(transaction);
            throw new RuntimeException("Transfer execution failed: " + extractErrorMessage(e), e);
        }
    }

    /**
     * Gets transactions history for the logged-in user.
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getMyTransactions(Long userId) {
        List<String> accounts = accountClient.getAccountNumbersByUserInternal(userId);
        if (accounts == null || accounts.isEmpty()) {
            return Collections.emptyList();
        }

        return transactionRepository.findBySourceAccountNumberInOrTargetAccountNumberInOrderByTimestampDesc(accounts, accounts).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AccountDto getAndValidateAccount(String accountNumber) {
        try {
            AccountDto account = accountClient.getAccountInternal(accountNumber);
            if (account == null) {
                throw new IllegalArgumentException("Account not found: " + accountNumber);
            }
            if (account.isFrozen()) {
                throw new IllegalArgumentException("Account is frozen: " + accountNumber);
            }
            return account;
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Account not found: " + accountNumber);
        }
    }

    private void publishCompletedEvent(Transaction transaction) {
        try {
            TransactionEvent event = TransactionEvent.builder()
                    .transactionId(transaction.getTransactionId())
                    .sourceAccountId(transaction.getSourceAccountNumber())
                    .targetAccountId(transaction.getTargetAccountNumber())
                    .amount(transaction.getAmount())
                    .transactionType(transaction.getTransactionType().name())
                    .channel(transaction.getChannel().name())
                    .status(transaction.getStatus().name())
                    .timestamp(transaction.getTimestamp())
                    .build();

            kafkaTemplate.send(TOPIC_TRANSACTION_COMPLETED, transaction.getTransactionId(), event);
            log.info("Successfully published transaction completed event to Kafka for transaction: {}", transaction.getTransactionId());
        } catch (Exception e) {
            // Note: In production we would write to outbox table or retry. Here we log and proceed.
            log.error("Failed to publish transaction event to Kafka: {}", e.getMessage());
        }
    }

    private String extractErrorMessage(Exception e) {
        if (e instanceof FeignException) {
            FeignException fe = (FeignException) e;
            return "Account Service error (HTTP " + fe.status() + ")";
        }
        return e.getMessage() != null ? e.getMessage() : "Unknown error";
    }

    private boolean isAdminOrEmployee(String roles) {
        return roles != null && (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_EMPLOYEE"));
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .sourceAccountNumber(transaction.getSourceAccountNumber())
                .targetAccountNumber(transaction.getTargetAccountNumber())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .channel(transaction.getChannel())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .userId(transaction.getUserId())
                .timestamp(transaction.getTimestamp())
                .build();
    }
}
