package com.baro13.readfast.infrastructure.storage.service;

import com.baro13.readfast.infrastructure.storage.port.AnalyticsStorage;
import com.baro13.readfast.infrastructure.storage.port.StorageType;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 분석용 스토리지 서비스
 * 분석 전용 기능들을 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {
    
    private final List<AnalyticsStorage> analyticsStorages;
    
    /**
     * 분석용 통합 데이터베이스 생성
     */
    public void createAnalyticsDatabase(LocalDate startDate, LocalDate endDate) {
        for (AnalyticsStorage storage : analyticsStorages) {
            try {
                storage.createAnalyticsDatabase(startDate, endDate);
                log.info("{}에서 분석용 DB 생성 완료: {} ~ {}", 
                    storage.getStorageType(), startDate, endDate);
            } catch (Exception e) {
                log.error("{}에서 분석용 DB 생성 실패: {} ~ {}", 
                    storage.getStorageType(), startDate, endDate, e);
            }
        }
    }
    
    /**
     * 압축 파일을 분석용 형태로 변환
     */
    public void convertCompressedFile(java.nio.file.Path compressedFilePath, LocalDate date) {
        for (AnalyticsStorage storage : analyticsStorages) {
            try {
                storage.convertCompressedFile(compressedFilePath, date);
                log.info("{}에서 압축 파일 변환 완료: {}", 
                    storage.getStorageType(), compressedFilePath);
            } catch (Exception e) {
                log.error("{}에서 압축 파일 변환 실패: {}", 
                    storage.getStorageType(), compressedFilePath, e);
            }
        }
    }
    
    /**
     * 기본 분석 스토리지 반환 (SQLite)
     */
    public AnalyticsStorage getDefaultAnalyticsStorage() {
        return analyticsStorages.stream()
            .filter(storage -> storage.getStorageType() == StorageType.SQLITE)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("기본 분석 스토리지(SQLite)를 찾을 수 없습니다"));
    }
}