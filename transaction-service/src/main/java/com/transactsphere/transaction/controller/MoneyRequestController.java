package com.transactsphere.transaction.controller;

import com.transactsphere.transaction.dto.CreateMoneyRequestDto;
import com.transactsphere.transaction.dto.MoneyRequestDto;
import com.transactsphere.transaction.dto.TransactionResponse;
import com.transactsphere.transaction.service.MoneyRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class MoneyRequestController {

    private final MoneyRequestService moneyRequestService;

    @PostMapping
    public ResponseEntity<MoneyRequestDto> createRequest(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateMoneyRequestDto request) {
        return new ResponseEntity<>(moneyRequestService.createMoneyRequest(userId, request), HttpStatus.CREATED);
    }

    @GetMapping("/incoming")
    public ResponseEntity<List<MoneyRequestDto>> getIncomingRequests(
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(moneyRequestService.getIncomingRequests(username));
    }

    @GetMapping("/outgoing")
    public ResponseEntity<List<MoneyRequestDto>> getOutgoingRequests(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("accountNumber") String accountNumber) {
        return ResponseEntity.ok(moneyRequestService.getOutgoingRequests(userId, accountNumber));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<TransactionResponse> acceptRequest(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @RequestParam("sourceAccountNumber") String sourceAccountNumber) {
        return ResponseEntity.ok(moneyRequestService.acceptMoneyRequest(userId, id, sourceAccountNumber));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<MoneyRequestDto> rejectRequest(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(moneyRequestService.rejectMoneyRequest(userId, id));
    }
}
