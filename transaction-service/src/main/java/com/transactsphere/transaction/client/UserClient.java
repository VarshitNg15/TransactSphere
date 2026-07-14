package com.transactsphere.transaction.client;

import com.transactsphere.transaction.dto.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/internal/users/{id}")
    UserProfileResponse getUserInternal(@PathVariable("id") Long id);
}
