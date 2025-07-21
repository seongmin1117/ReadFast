package com.baro13.readfast.infrastructure.storage.port;

import com.baro13.readfast.domain.AuthLog;
import java.time.LocalDate;
import java.util.List;

/**
 * 분석용 스토리지 전용 인터페이스
 * 기본 DataStorage 기능에 분석 전용 기능을 추가
 */
public interface AnalyticsStorage extends DataStorage {
    
    /**
     * 특정 사용자의 인증 로그 조회
     */
    List<AuthLog> retrieveByUserId(LocalDate date, String userId);
    
    /**
     * 특정 사용자의 인증 로그 개수 조회
     */
    long countByUserId(LocalDate date, String userId);
    
    /**
     * 분석용 통합 데이터베이스 생성
     */
    void createAnalyticsDatabase(LocalDate startDate, LocalDate endDate);
    
    /**
     * 압축 파일을 분석용 형태로 변환
     */
    void convertCompressedFile(java.nio.file.Path compressedFilePath, LocalDate date);
}