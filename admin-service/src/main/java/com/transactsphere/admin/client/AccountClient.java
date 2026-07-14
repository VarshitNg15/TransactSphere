package com.transactsphere.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "account-service")
public interface AccountClient {
    @PutMapping("/api/v1/accounts/{accountNumber}/freeze")
    Object freezeAccount(
            @PathVariable("accountNumber") String accountNumber,
            @RequestParam("freeze") boolean freeze,
            @RequestHeader("X-User-Roles") String roles);
}
