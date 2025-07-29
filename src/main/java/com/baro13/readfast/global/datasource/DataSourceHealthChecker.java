package com.baro13.readfast.global.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSourceHealthChecker {

    private final DataSource masterDataSource;
    private final DataSource slaveDataSource;

    public Map<String, Object> health() {
        try {
            boolean masterHealthy = checkDataSourceHealth(masterDataSource, "master");
            boolean slaveHealthy = checkDataSourceHealth(slaveDataSource, "slave");

            if (masterHealthy && slaveHealthy) {
                return Map.of(
                    "status", "UP",
                    "master", "UP",
                    "slave", "UP",
                    "message", "Both master and slave databases are healthy"
                );
            } else if (masterHealthy) {
                return Map.of(
                    "status", "DEGRADED",
                    "master", "UP",
                    "slave", "DOWN",
                    "message", "Master is healthy but slave is down"
                );
            } else {
                return Map.of(
                    "status", "DOWN",
                    "master", masterHealthy ? "UP" : "DOWN",
                    "slave", slaveHealthy ? "UP" : "DOWN",
                    "message", "Master database is down"
                );
            }
        } catch (Exception e) {
            log.error("Health check failed", e);
            return Map.of(
                "status", "DOWN",
                "error", e.getMessage()
            );
        }
    }

    private boolean checkDataSourceHealth(DataSource dataSource, String name) {
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(5); // 5초 타임아웃
            log.debug("{} database health check: {}", name, valid ? "HEALTHY" : "UNHEALTHY");
            return valid;
        } catch (SQLException e) {
            log.warn("{} database health check failed: {}", name, e.getMessage());
            return false;
        }
    }

    /**
     * 개별 데이터소스 상태 체크
     */
    public Map<String, Object> getDataSourceStatus() {
        boolean masterHealthy = checkDataSourceHealth(masterDataSource, "master");
        boolean slaveHealthy = checkDataSourceHealth(slaveDataSource, "slave");

        return Map.of(
            "masterAvailable", masterHealthy,
            "slaveAvailable", slaveHealthy,
            "failureCount", (!masterHealthy || !slaveHealthy) ? 1 : 0,
            "lastUpdate", java.time.Instant.now().toString()
        );
    }
}