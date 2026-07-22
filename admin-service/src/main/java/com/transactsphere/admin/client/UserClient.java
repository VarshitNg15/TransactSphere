package com.transactsphere.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", url = "http://user-service:8082")
public interface UserClient {
    @PutMapping("/api/v1/users/{id}/kyc")
    Object updateKyc(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            @RequestHeader("X-User-Roles") String roles);
}
