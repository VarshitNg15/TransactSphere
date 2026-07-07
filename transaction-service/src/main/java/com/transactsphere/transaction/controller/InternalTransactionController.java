package com.transactsphere.transaction.controller;

import com.transactsphere.transaction.dto.TransactionResponse;
import com.transactsphere.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/transactions")
@RequiredArgsConstructor
public class InternalTransactionController {

    private final TransactionService transactionService;

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsInternal(@PathVariable("accountNumber") String accountNumber) {
        List<TransactionResponse> response = transactionService.getTransactionsByAccountNumber(accountNumber);
        return ResponseEntity.ok(response);
    }
}
