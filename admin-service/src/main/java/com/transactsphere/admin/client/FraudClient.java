package com.transactsphere.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "fraud-service", url = "http://fraud-service:8086")
public interface FraudClient {
    @GetMapping("/api/v1/fraud/logs")
    List<Object> getAllLogs(@RequestHeader("X-User-Roles") String roles);

    @PutMapping("/api/v1/fraud/resolve/{id}")
    Object resolveLog(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Name") String username,
            @RequestHeader("X-User-Roles") String roles);
}
