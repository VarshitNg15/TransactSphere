package com.transactsphere.analytics.client;

import com.transactsphere.analytics.dto.AccountResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "account-service")
public interface AccountClient {
    @GetMapping("/internal/accounts/{accountNumber}")
    AccountResponse getAccountInternal(@PathVariable("accountNumber") String accountNumber);
}
