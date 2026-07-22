package com.transactsphere.statement.client;

import com.transactsphere.statement.dto.AccountResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "account-service", url = "http://account-service:8083")
public interface AccountClient {
    @GetMapping("/internal/accounts/{accountNumber}")
    AccountResponse getAccountInternal(@PathVariable("accountNumber") String accountNumber);
}
