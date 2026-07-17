package com.transactsphere.transaction.service;

import com.transactsphere.transaction.client.AccountClient;
import com.transactsphere.transaction.client.UserClient;
import com.transactsphere.transaction.dto.*;
import com.transactsphere.transaction.model.*;
import com.transactsphere.transaction.repository.TransactionRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transactsphere.transaction.repository.OutboxEventRepository;
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
    private final UserClient userClient;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    private static final BigDecimal MIN_LIMIT = new BigDecimal("1.00");
    private static final BigDecimal MAX_LIMIT = new BigDecimal("50000.00");
    private static final String TOPIC_TRANSACTION_COMPLETED = "transaction.completed";
    private static final String TOPIC_TRANSACTION_FRAUD = "transaction.fraudulent";



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

        String fraudReason = checkFraud(transaction, targetAccount);
        if (fraudReason != null) {
            transaction.setStatus(TransactionStatus.FRAUDULENT);
            transaction.setDescription(request.getDescription() + " (Flagged as Fraudulent)");
            transactionRepository.save(transaction);
            publishFraudEvent(transaction, fraudReason);
            throw new RuntimeException("Transfer blocked due to suspected fraud.");
        }

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

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByAccountNumber(String accountNumber) {
        return transactionRepository.findBySourceAccountNumberInOrTargetAccountNumberInOrderByTimestampDesc(
                List.of(accountNumber), List.of(accountNumber)
        ).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
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

            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(transaction.getTransactionId())
                    .eventType(TOPIC_TRANSACTION_COMPLETED)
                    .payload(payload)
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Successfully saved transaction completed event to outbox for transaction: {}", transaction.getTransactionId());
        } catch (Exception e) {
            log.error("Failed to save transaction event to outbox: {}", e.getMessage());
            throw new RuntimeException("Failed to save outbox event", e);
        }
    }

    private String checkFraud(Transaction transaction, AccountDto targetAccount) {
        BigDecimal amount = transaction.getAmount();
        Long userId = transaction.getUserId();
        LocalDateTime now = LocalDateTime.now();

        // 1. Check target account KYC (if provided)
        if (targetAccount != null) {
            try {
                UserProfileResponse targetUser = userClient.getUserInternal(targetAccount.getUserId());
                if (targetUser == null || targetUser.getKycStatus() == null || !targetUser.getKycStatus().equals("APPROVED")) {
                    return "Target account KYC is missing or not approved";
                }
            } catch (Exception e) {
                log.warn("Failed to fetch KYC status for target account user: {}", e.getMessage());
                return "Failed to verify target account KYC";
            }
        }

        // 2. High Frequency: > 5 transactions in last 10 minutes
        LocalDateTime tenMinsAgo = now.minusMinutes(10);
        List<Transaction> recentTransactions = transactionRepository.findByUserIdAndTimestampAfter(userId, tenMinsAgo);
        if (recentTransactions.size() >= 5) {
            return "High Frequency of Transactions";
        }

        // 3. 24-hour limit: > 100,000
        LocalDateTime oneDayAgo = now.minusHours(24);
        List<Transaction> dailyTransactions = transactionRepository.findByUserIdAndTimestampAfter(userId, oneDayAgo);
        BigDecimal dailyTotal = dailyTransactions.stream()
                .filter(t -> t.getStatus() != TransactionStatus.FAILED && t.getStatus() != TransactionStatus.FRAUDULENT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (dailyTotal.add(amount).compareTo(new BigDecimal("100000.00")) > 0) {
            return "24-Hour Transaction Limit Exceeded";
        }

        return null;
    }

    private void publishFraudEvent(Transaction transaction, String fraudReason) {
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
                    .fraudReason(fraudReason)
                    .build();

            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(transaction.getTransactionId())
                    .eventType(TOPIC_TRANSACTION_FRAUD)
                    .payload(payload)
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Successfully saved transaction fraudulent event to outbox for transaction: {}", transaction.getTransactionId());
        } catch (Exception e) {
            log.error("Failed to save transaction fraudulent event to outbox: {}", e.getMessage());
            throw new RuntimeException("Failed to save outbox event", e);
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

    @Transactional
    public TransactionResponse deposit(Long adminId, String roles, DepositRequest request) {
        if (!isAdminOrEmployee(roles)) {
            throw new IllegalArgumentException("Only Admin or Employee can perform deposit");
        }
        BigDecimal amount = request.getAmount();

        if (amount.compareTo(MIN_LIMIT) < 0) {
            throw new IllegalArgumentException("Deposit amount must be at least ₹" + MIN_LIMIT);
        }

        AccountDto targetAccount = getAndValidateAccount(request.getTargetAccountNumber());

        String txId = "TXN-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase();
        Transaction transaction = Transaction.builder()
                .transactionId(txId)
                .targetAccountNumber(request.getTargetAccountNumber())
                .amount(amount)
                .transactionType(TransactionType.DEPOSIT)
                .channel(TransactionChannel.WEB)
                .status(TransactionStatus.PENDING)
                .description(request.getDescription() != null ? request.getDescription() : "Admin Deposit")
                .userId(targetAccount.getUserId())
                .build();

        transaction = transactionRepository.save(transaction);

        try {
            InternalTransferRequest transferReq = InternalTransferRequest.builder()
                    .targetAccountNumber(request.getTargetAccountNumber())
                    .amount(amount)
                    .build();
            accountClient.transferInternal(transferReq);

            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction = transactionRepository.save(transaction);

            publishCompletedEvent(transaction);
            return mapToResponse(transaction);
        } catch (Exception e) {
            log.error("Failed to complete deposit. Error: {}", e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setDescription((request.getDescription() != null ? request.getDescription() : "Admin Deposit") + " (Failed: " + extractErrorMessage(e) + ")");
            transactionRepository.save(transaction);
            throw new RuntimeException("Deposit execution failed: " + extractErrorMessage(e), e);
        }
    }

    @Transactional
    public TransactionResponse withdraw(Long adminId, String roles, WithdrawRequest request) {
        if (!isAdminOrEmployee(roles)) {
            throw new IllegalArgumentException("Only Admin or Employee can perform withdrawal");
        }
        BigDecimal amount = request.getAmount();

        if (amount.compareTo(MIN_LIMIT) < 0) {
            throw new IllegalArgumentException("Withdrawal amount must be at least ₹" + MIN_LIMIT);
        }

        AccountDto sourceAccount = getAndValidateAccount(request.getSourceAccountNumber());

        String txId = "TXN-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase();
        Transaction transaction = Transaction.builder()
                .transactionId(txId)
                .sourceAccountNumber(request.getSourceAccountNumber())
                .amount(amount)
                .transactionType(TransactionType.WITHDRAWAL)
                .channel(TransactionChannel.WEB)
                .status(TransactionStatus.PENDING)
                .description(request.getDescription() != null ? request.getDescription() : "Admin Withdrawal")
                .userId(sourceAccount.getUserId())
                .build();

        transaction = transactionRepository.save(transaction);

        try {
            InternalTransferRequest transferReq = InternalTransferRequest.builder()
                    .sourceAccountNumber(request.getSourceAccountNumber())
                    .amount(amount)
                    .build();
            accountClient.transferInternal(transferReq);

            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction = transactionRepository.save(transaction);

            publishCompletedEvent(transaction);
            return mapToResponse(transaction);
        } catch (Exception e) {
            log.error("Failed to complete withdrawal. Error: {}", e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setDescription((request.getDescription() != null ? request.getDescription() : "Admin Withdrawal") + " (Failed: " + extractErrorMessage(e) + ")");
            transactionRepository.save(transaction);
            throw new RuntimeException("Withdrawal execution failed: " + extractErrorMessage(e), e);
        }
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
