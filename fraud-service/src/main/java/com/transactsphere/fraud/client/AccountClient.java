package com.transactsphere.fraud.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "account-service", url = "http://account-service:8083")
public interface AccountClient {
    @GetMapping("/internal/accounts/user/{userId}")
    List<String> getAccountNumbersByUserInternal(@PathVariable("userId") Long userId);
}
