package com.transactsphere.notification.client;

import com.transactsphere.notification.dto.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "http://user-service:8082")
public interface UserClient {
    @GetMapping("/internal/users/{id}")
    UserProfileResponse getUserInternal(@PathVariable("id") Long id);
}
