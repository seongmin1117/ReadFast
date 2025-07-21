package com.baro13.readfast.infrastructure.storage.port;

import com.baro13.readfast.domain.AuthLog;
import java.time.LocalDate;
import java.util.List;

/**
 * 기본 스토리지 인터페이스
 * 모든 스토리지 구현체가 구현해야 하는 기본 CRUD 연산을 정의
 */
public interface DataStorage {
    
    /**
     * 인증 로그 데이터를 특정 날짜로 저장
     */
    void store(List<AuthLog> authLogs, LocalDate date);
    
    /**
     * 특정 날짜의 인증 로그 조회
     */
    List<AuthLog> retrieve(LocalDate date);
    
    /**
     * 날짜 범위로 인증 로그 조회
     */
    List<AuthLog> retrieveByDateRange(LocalDate startDate, LocalDate endDate);
    
    /**
     * 특정 날짜의 데이터 존재 여부 확인
     */
    boolean exists(LocalDate date);
    
    /**
     * 특정 날짜의 데이터 삭제
     */
    void delete(LocalDate date);
    
    /**
     * 스토리지 타입 반환
     */
    StorageType getStorageType();
}