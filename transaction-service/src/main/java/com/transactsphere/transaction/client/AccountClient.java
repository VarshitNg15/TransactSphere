package com.transactsphere.transaction.client;

import com.transactsphere.transaction.dto.AccountDto;
import com.transactsphere.transaction.dto.InternalTransferRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "account-service")
public interface AccountClient {

    @GetMapping("/internal/accounts/{accountNumber}")
    AccountDto getAccountInternal(@PathVariable("accountNumber") String accountNumber);

    @PutMapping("/internal/accounts/transfer")
    void transferInternal(@RequestBody InternalTransferRequest request);

    @GetMapping("/internal/accounts/user/{userId}")
    List<String> getAccountNumbersByUserInternal(@PathVariable("userId") Long userId);
}
