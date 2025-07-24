package com.baro13.readfast.global.datasource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSourceMonitor {

    private final DataSource masterDataSource;
    private final DataSource slaveDataSource;

    private final AtomicInteger masterFailureCount = new AtomicInteger(0);
    private final AtomicInteger slaveFailureCount = new AtomicInteger(0);

    /**
     * 5분마다 데이터소스 연결 상태 모니터링
     */
    @Scheduled(fixedRate = 300000) // 5분
    public void monitorDataSources() {
        checkMasterDataSource();
        checkSlaveDataSource();
        logConnectionStats();
    }

    private void checkMasterDataSource() {
        try {
            if (isDataSourceHealthy(masterDataSource, "master")) {
                masterFailureCount.set(0);
            }
        } catch (Exception e) {
            int failures = masterFailureCount.incrementAndGet();
            log.error("Master database connection failed (failures: {}): {}", failures, e.getMessage());
            
            if (failures >= 3) {
                log.error("CRITICAL: Master database has failed {} times consecutively", failures);
                // 여기서 알림 시스템 호출 가능
            }
        }
    }

    private void checkSlaveDataSource() {
        try {
            if (isDataSourceHealthy(slaveDataSource, "slave")) {
                slaveFailureCount.set(0);
            }
        } catch (Exception e) {
            int failures = slaveFailureCount.incrementAndGet();
            log.warn("Slave database connection failed (failures: {}): {}", failures, e.getMessage());
            
            if (failures >= 5) {
                log.warn("WARNING: Slave database has failed {} times consecutively. Read queries will be routed to master", failures);
            }
        }
    }

    private boolean isDataSourceHealthy(DataSource dataSource, String name) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {
            
            boolean healthy = rs.next() && rs.getInt(1) == 1;
            log.debug("{} database health check: {}", name, healthy ? "OK" : "FAILED");
            return healthy;
        }
    }

    private void logConnectionStats() {
        try {
            int masterActiveConnections = getActiveConnectionCount(masterDataSource);
            int slaveActiveConnections = getActiveConnectionCount(slaveDataSource);
            
            log.info("Connection Stats - Master: {} active, Slave: {} active", 
                    masterActiveConnections, slaveActiveConnections);
                    
        } catch (Exception e) {
            log.warn("Failed to get connection statistics: {}", e.getMessage());
        }
    }

    private int getActiveConnectionCount(DataSource dataSource) {
        // HikariCP의 경우 getActiveConnections() 메소드 사용 가능
        if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
            com.zaxxer.hikari.HikariDataSource hikariDS = (com.zaxxer.hikari.HikariDataSource) dataSource;
            return hikariDS.getHikariPoolMXBean().getActiveConnections();
        }
        return -1; // 지원하지 않는 경우
    }

    /**
     * Slave 데이터베이스 사용 가능 여부 체크
     */
    public boolean isSlaveAvailable() {
        return slaveFailureCount.get() < 3;
    }

    /**
     * Master 데이터베이스 사용 가능 여부 체크
     */
    public boolean isMasterAvailable() {
        return masterFailureCount.get() < 3;
    }

    /**
     * 현재 실패 카운트 조회
     */
    public String getFailureStatus() {
        return String.format("Master failures: %d, Slave failures: %d", 
                masterFailureCount.get(), slaveFailureCount.get());
    }
}