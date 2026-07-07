package com.transactsphere.analytics.repository;

import com.transactsphere.analytics.model.UserAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface UserAnalyticsRepository extends JpaRepository<UserAnalytics, Long> {

    @Query("SELECT SUM(ua.totalVolume) FROM UserAnalytics ua")
    BigDecimal sumTotalVolume();

    @Query("SELECT SUM(ua.totalCount) FROM UserAnalytics ua")
    Long sumTotalCount();

    @Query("SELECT COUNT(ua) FROM UserAnalytics ua")
    Long countActiveUsers();
}
