package com.baro13.readfast.infrastructure.storage;

import com.baro13.readfast.infrastructure.batch.config.DataRetentionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsStorageFactory {
    
    private final DataRetentionProperties properties;
    private final SQLiteStorage sqliteStorage;
    
    /**
     * SQLite 분석 스토리지 반환
     */
    public DataStorage getAnalyticsStorage() {
        log.info("SQLite 분석 스토리지 사용");
        return sqliteStorage;
    }
    
    /**
     * 압축 파일을 SQLite로 변환
     */
    public void convertCompressedFile(java.nio.file.Path compressedFilePath, java.time.LocalDate date) {
        try {
            sqliteStorage.convertFromCompressedFile(compressedFilePath, date);
        } catch (Exception e) {
            log.error("압축 파일 SQLite 변환 실패: {}", compressedFilePath, e);
            throw new RuntimeException("압축 파일 SQLite 변환 실패", e);
        }
    }
    
    /**
     * 분석용 통합 SQLite DB 생성
     */
    public void createAnalyticsDatabase(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        try {
            sqliteStorage.createAnalyticsDatabase(startDate, endDate);
        } catch (Exception e) {
            log.error("분석용 통합 SQLite DB 생성 실패", e);
            throw new RuntimeException("분석용 통합 SQLite DB 생성 실패", e);
        }
    }
    
    /**
     * 특정 사용자의 인증 로그 조회 (성능 최적화)
     */
    public java.util.List<com.baro13.readfast.domain.AuthLog> retrieveByUserId(
            java.time.LocalDate date, String userId) {
        
        try {
            return sqliteStorage.retrieveByUserId(date, userId);
        } catch (Exception e) {
            log.error("사용자별 SQLite 조회 실패: {} -> {}", date, userId, e);
            return java.util.Collections.emptyList();
        }
    }
    
    /**
     * 사용자별 인증 로그 개수 조회 (성능 최적화)
     */
    public long countByUserId(java.time.LocalDate date, String userId) {
        try {
            return sqliteStorage.countByUserId(date, userId);
        } catch (Exception e) {
            log.error("사용자별 SQLite 카운트 실패: {} -> {}", date, userId, e);
            return 0;
        }
    }
    
    /**
     * 현재 설정된 분석 DB 타입 반환
     */
    public String getAnalyticsDbType() {
        return "sqlite";
    }
}