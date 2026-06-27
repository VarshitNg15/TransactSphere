package com.transactsphere.account.controller;

import com.transactsphere.account.dto.AccountResponse;
import com.transactsphere.account.dto.InternalTransferRequest;
import com.transactsphere.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/accounts")
@RequiredArgsConstructor
public class InternalAccountController {

    private final AccountService accountService;

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountInternal(@PathVariable("accountNumber") String accountNumber) {
        AccountResponse response = accountService.getAccountByAccountNumber(accountNumber);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/transfer")
    public ResponseEntity<Void> transferInternal(@Valid @RequestBody InternalTransferRequest request) {
        accountService.executeTransfer(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<String>> getAccountNumbersByUserInternal(@PathVariable("userId") Long userId) {
        List<String> accountNumbers = accountService.getAccountsByUserId(userId).stream()
                .map(AccountResponse::getAccountNumber)
                .collect(Collectors.toList());
        return ResponseEntity.ok(accountNumbers);
    }
}
