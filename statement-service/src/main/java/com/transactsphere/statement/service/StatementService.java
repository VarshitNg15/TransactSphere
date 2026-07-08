package com.transactsphere.statement.service;

import com.transactsphere.statement.client.AccountClient;
import com.transactsphere.statement.client.TransactionClient;
import com.transactsphere.statement.dto.AccountResponse;
import com.transactsphere.statement.dto.TransactionResponse;
import com.transactsphere.statement.model.StatementLog;
import com.transactsphere.statement.repository.StatementLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatementService {

    private final TransactionClient transactionClient;
    private final AccountClient accountClient;
    private final StatementLogRepository statementLogRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Map<String, Object> generateStatement(String accountNumber, LocalDateTime startDate, LocalDateTime endDate, Long userId) {
        log.info("Generating statement for account: {} from {} to {}", accountNumber, startDate, endDate);

        // Fetch account
        AccountResponse account = accountClient.getAccountInternal(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("Account not found: " + accountNumber);
        }

        // Verify ownership
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("User does not own this account.");
        }

        // Fetch transactions
        List<TransactionResponse> allTx = transactionClient.getTransactionsInternal(accountNumber);

        // Filter transactions within range
        List<TransactionResponse> filteredTx = allTx.stream()
                .filter(t -> t.getTimestamp() != null &&
                        !t.getTimestamp().isBefore(startDate) &&
                        !t.getTimestamp().isAfter(endDate) &&
                        "COMPLETED".equalsIgnoreCase(t.getStatus()))
                .collect(Collectors.toList());

        // Calculate summary
        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;

        for (TransactionResponse tx : filteredTx) {
            if ("DEPOSIT".equalsIgnoreCase(tx.getTransactionType())) {
                totalDeposits = totalDeposits.add(tx.getAmount());
            } else if ("WITHDRAWAL".equalsIgnoreCase(tx.getTransactionType())) {
                totalWithdrawals = totalWithdrawals.add(tx.getAmount());
            } else if ("TRANSFER".equalsIgnoreCase(tx.getTransactionType())) {
                if (accountNumber.equals(tx.getSourceAccountNumber())) {
                    totalWithdrawals = totalWithdrawals.add(tx.getAmount());
                } else if (accountNumber.equals(tx.getTargetAccountNumber())) {
                    totalDeposits = totalDeposits.add(tx.getAmount());
                }
            }
        }

        BigDecimal netChange = totalDeposits.subtract(totalWithdrawals);

        // Log statement generation
        StatementLog statementLog = StatementLog.builder()
                .userId(userId)
                .accountNumber(accountNumber)
                .startDate(startDate)
                .endDate(endDate)
                .generatedAt(LocalDateTime.now())
                .build();
        statementLogRepository.save(statementLog);

        try {
            com.transactsphere.statement.dto.GenericEvent event = com.transactsphere.statement.dto.GenericEvent.builder()
                    .userId(userId)
                    .message("A new account statement for account " + accountNumber + " has been generated.")
                    .build();
            kafkaTemplate.send("notification.generic", event);
        } catch (Exception e) {
            // Ignore kafka exceptions
        }

        Map<String, Object> result = new HashMap<>();
        result.put("accountNumber", accountNumber);
        result.put("accountType", account.getAccountType());
        result.put("currentBalance", account.getBalance());
        result.put("totalDeposits", totalDeposits);
        result.put("totalWithdrawals", totalWithdrawals);
        result.put("netChange", netChange);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("transactions", filteredTx);

        return result;
    }

    @SuppressWarnings("unchecked")
    public String generateCsvStatement(Map<String, Object> statementData) {
        StringBuilder sb = new StringBuilder();
        sb.append("Account Statement,Account Number: ").append(statementData.get("accountNumber")).append("\n");
        sb.append("Statement Period,").append(statementData.get("startDate")).append(" to ").append(statementData.get("endDate")).append("\n");
        sb.append("Current Balance,").append(statementData.get("currentBalance")).append("\n");
        sb.append("Total Deposits,").append(statementData.get("totalDeposits")).append("\n");
        sb.append("Total Withdrawals,").append(statementData.get("totalWithdrawals")).append("\n");
        sb.append("Net Change,").append(statementData.get("netChange")).append("\n\n");

        sb.append("Transaction ID,Date,Type,Amount,Status,Description\n");
        List<TransactionResponse> txList = (List<TransactionResponse>) statementData.get("transactions");
        for (TransactionResponse tx : txList) {
            sb.append(tx.getTransactionId()).append(",")
                    .append(tx.getTimestamp()).append(",")
                    .append(tx.getTransactionType()).append(",")
                    .append(tx.getAmount()).append(",")
                    .append(tx.getStatus()).append(",")
                    .append(tx.getDescription() != null ? tx.getDescription().replace(",", " ") : "").append("\n");
        }
        return sb.toString();
    }
}
