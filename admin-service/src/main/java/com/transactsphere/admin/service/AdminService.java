package com.transactsphere.admin.service;

import com.transactsphere.admin.client.AccountClient;
import com.transactsphere.admin.client.AnalyticsClient;
import com.transactsphere.admin.client.FraudClient;
import com.transactsphere.admin.client.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserClient userClient;
    private final AccountClient accountClient;
    private final FraudClient fraudClient;
    private final AnalyticsClient analyticsClient;

    public Map<String, Object> getPlatformStats(String roles) {
        log.info("Fetching global platform statistics");
        Map<String, Object> stats = new HashMap<>();
        try {
            Map<String, Object> dashboard = analyticsClient.getDashboard(roles);
            stats.putAll(dashboard);
        } catch (Exception e) {
            log.error("Failed to fetch dashboard stats from analytics-service: {}", e.getMessage());
            stats.put("totalVolume", 0);
            stats.put("totalCount", 0);
            stats.put("activeUsers", 0);
        }

        try {
            List<Object> fraudLogs = fraudClient.getAllLogs(roles);
            stats.put("totalFraudIncidents", fraudLogs.size());
            stats.put("unresolvedFraudCount", fraudLogs.stream()
                    .filter(logObj -> logObj instanceof Map && Boolean.FALSE.equals(((Map<?, ?>) logObj).get("resolved")))
                    .count());
        } catch (Exception e) {
            log.error("Failed to fetch fraud stats from fraud-service: {}", e.getMessage());
            stats.put("totalFraudIncidents", 0);
            stats.put("unresolvedFraudCount", 0);
        }

        return stats;
    }

    public Object updateKycStatus(Long userId, String status, String roles) {
        return userClient.updateKyc(userId, status, roles);
    }

    public Object setAccountFreezeStatus(String accountNumber, boolean freeze, String roles) {
        return accountClient.freezeAccount(accountNumber, freeze, roles);
    }

    public List<Object> getFraudLogs(String roles) {
        return fraudClient.getAllLogs(roles);
    }

    public Object resolveFraudIncident(Long id, String username, String roles) {
        return fraudClient.resolveLog(id, username, roles);
    }
}
