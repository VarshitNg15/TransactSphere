package com.transactsphere.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "analytics-service")
public interface AnalyticsClient {
    @GetMapping("/api/v1/analytics/dashboard")
    Map<String, Object> getDashboard(@RequestHeader("X-User-Roles") String roles);
}
