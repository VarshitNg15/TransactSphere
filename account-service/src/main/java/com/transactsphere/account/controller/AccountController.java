package com.transactsphere.account.controller;

import com.transactsphere.account.dto.AccountCreateRequest;
import com.transactsphere.account.dto.AccountResponse;
import com.transactsphere.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AccountCreateRequest request) {
        AccountResponse response = accountService.createAccount(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(
            @RequestHeader("X-User-Id") Long userId) {
        List<AccountResponse> response = accountService.getAccountsByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable("accountNumber") String accountNumber,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Roles") String roles) {
        AccountResponse response = accountService.getAccountByAccountNumber(accountNumber);
        if (!response.getUserId().equals(userId) && !isAdminOrEmployee(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{accountNumber}/freeze")
    public ResponseEntity<AccountResponse> freezeAccount(
            @PathVariable("accountNumber") String accountNumber,
            @RequestParam("freeze") boolean freeze,
            @RequestHeader("X-User-Roles") String roles) {
        if (!isAdminOrEmployee(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        AccountResponse response = accountService.setFreezeStatus(accountNumber, freeze);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/user/{userId}/freeze")
    public ResponseEntity<List<AccountResponse>> freezeUserAccounts(
            @PathVariable("userId") Long userId,
            @RequestParam("freeze") boolean freeze,
            @RequestHeader("X-User-Roles") String roles) {
        if (!isAdminOrEmployee(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<AccountResponse> accounts = accountService.getAccountsByUserId(userId);
        for (AccountResponse acc : accounts) {
            accountService.setFreezeStatus(acc.getAccountNumber(), freeze);
        }
        return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<AccountResponse>> getAllAccounts(
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (!isAdminOrEmployee(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<AccountResponse> response = accountService.getAllAccounts();
        return ResponseEntity.ok(response);
    }

    private boolean isAdminOrEmployee(String roles) {
        return roles != null && (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_EMPLOYEE"));
    }
}
