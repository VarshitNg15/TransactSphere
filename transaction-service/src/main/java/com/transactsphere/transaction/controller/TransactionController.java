package com.transactsphere.transaction.controller;

import com.transactsphere.transaction.dto.DepositRequest;
import com.transactsphere.transaction.dto.TransactionResponse;
import com.transactsphere.transaction.dto.TransferRequest;
import com.transactsphere.transaction.dto.WithdrawRequest;
import com.transactsphere.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody DepositRequest request) {
        TransactionResponse response = transactionService.deposit(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Roles") String roles,
            @Valid @RequestBody WithdrawRequest request) {
        TransactionResponse response = transactionService.withdraw(userId, roles, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Roles") String roles,
            @Valid @RequestBody TransferRequest request) {
        TransactionResponse response = transactionService.transfer(userId, roles, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/my")
    public ResponseEntity<List<TransactionResponse>> getMyTransactions(
            @RequestHeader("X-User-Id") Long userId) {
        List<TransactionResponse> response = transactionService.getMyTransactions(userId);
        return ResponseEntity.ok(response);
    }
}
