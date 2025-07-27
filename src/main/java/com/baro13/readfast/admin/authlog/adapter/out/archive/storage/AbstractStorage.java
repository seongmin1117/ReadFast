package com.baro13.readfast.admin.authlog.adapter.out.archive.storage;

import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * 스토리지 성능 모니터링을 제공하는 추상 클래스
 * 모든 스토리지 구현체가 상속하여 성능 측정 기능을 사용할 수 있음
 */
@Slf4j
public abstract class AbstractStorage implements Storage {

    protected void logStoragePerformance(String operation, LocalDate date, int recordCount, long executionTimeMs) {
        var throughputPerSecond = executionTimeMs > 0 ? (recordCount * 1000.0 / executionTimeMs) : 0;
        
        log.info("스토리지 성능 - 포맷: {}, 작업: {}, 날짜: {}, 레코드: {}, " +
                "실행시간: {}ms, 처리량: {:.2f} records/sec", 
                getArchiveFormat(), operation, date, recordCount, executionTimeMs, throughputPerSecond);
    }
    
    protected void logCompressionPerformance(String operation, long originalSize, long compressedSize, long executionTimeMs) {
        var compressionRatio = originalSize > 0 ? (double) compressedSize / originalSize * 100 : 100;
        var throughputMBPerSecond = executionTimeMs > 0 ? (originalSize / 1024.0 / 1024.0) / (executionTimeMs / 1000.0) : 0;
        
        log.info("압축 성능 - 작업: {}, 원본: {}bytes, 압축후: {}bytes, 압축비: {:.1f}%, " +
                "실행시간: {}ms, 처리량: {:.2f} MB/sec", 
                operation, originalSize, compressedSize, compressionRatio, executionTimeMs, throughputMBPerSecond);
    }
    
    /**
     * 성능 측정을 포함한 store 메서드
     */
    @Override
    public final void store(List<AuthLog> authLogs, LocalDate date) {
        var startTime = System.currentTimeMillis();
        
        try {
            doStore(authLogs, date);
            
            var executionTime = System.currentTimeMillis() - startTime;
            logStoragePerformance("STORE", date, authLogs.size(), executionTime);
            
        } catch (Exception e) {
            var executionTime = System.currentTimeMillis() - startTime;
            log.error("스토리지 저장 실패 - 포맷: {}, 날짜: {}, 레코드: {}, 실행시간: {}ms", 
                     getArchiveFormat(), date, authLogs.size(), executionTime, e);
            throw e;
        }
    }
    
    /**
     * 성능 측정을 포함한 retrieve 메서드
     */
    @Override
    public final List<AuthLog> retrieve(LocalDate date) {
        var startTime = System.currentTimeMillis();
        
        try {
            var result = doRetrieve(date);
            
            var executionTime = System.currentTimeMillis() - startTime;
            logStoragePerformance("RETRIEVE", date, result.size(), executionTime);
            
            return result;
            
        } catch (Exception e) {
            var executionTime = System.currentTimeMillis() - startTime;
            log.error("스토리지 조회 실패 - 포맷: {}, 날짜: {}, 실행시간: {}ms", 
                     getArchiveFormat(), date, executionTime, e);
            throw e;
        }
    }
    
    /**
     * 실제 저장 구현
     */
    protected abstract void doStore(List<AuthLog> authLogs, LocalDate date);
    
    /**
     * 실제 조회 구현
     */
    protected abstract List<AuthLog> doRetrieve(LocalDate date);
}