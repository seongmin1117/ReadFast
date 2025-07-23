package com.baro13.readfast.admin.authlog.adapter.out.archive;

import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.admin.policy.domain.model.vo.ArchivingStrategy;
import java.time.LocalDate;
import java.util.List;

/**
 * 기본 스토리지 인터페이스
 * 모든 스토리지 구현체가 구현해야 하는 기본 CRUD 연산을 정의
 */
public interface DataStorage {
    
    /**
     * 인증 로그 데이터를 특정 날짜로 저장
     * 
     * @param authLogs 저장할 인증 로그 목록
     * @param date 저장 날짜
     * @throws IllegalArgumentException authLogs가 null이거나 비어있는 경우, date가 null인 경우
     * @throws RuntimeException 스토리지 저장 중 오류가 발생한 경우
     */
    void store(List<AuthLog> authLogs, LocalDate date);
    
    /**
     * 특정 날짜의 인증 로그 조회
     * 
     * @param date 조회할 날짜
     * @return 해당 날짜의 인증 로그 목록 (데이터가 없으면 빈 리스트)
     * @throws IllegalArgumentException date가 null인 경우
     * @throws RuntimeException 스토리지 조회 중 오류가 발생한 경우
     */
    List<AuthLog> retrieve(LocalDate date);
    
    /**
     * 날짜 범위로 인증 로그 조회
     * 
     * @param startDate 시작 날짜 (포함)
     * @param endDate 종료 날짜 (포함)
     * @return 해당 날짜 범위의 인증 로그 목록 (데이터가 없으면 빈 리스트)
     * @throws IllegalArgumentException startDate 또는 endDate가 null인 경우, startDate가 endDate보다 이후인 경우
     * @throws RuntimeException 스토리지 조회 중 오류가 발생한 경우
     */
    List<AuthLog> retrieveByDateRange(LocalDate startDate, LocalDate endDate);
    
    /**
     * 특정 날짜의 데이터 존재 여부 확인
     * 
     * @param date 확인할 날짜
     * @return 데이터 존재 여부
     * @throws IllegalArgumentException date가 null인 경우
     * @throws RuntimeException 스토리지 확인 중 오류가 발생한 경우
     */
    boolean exists(LocalDate date);
    
    /**
     * 특정 날짜의 데이터 삭제
     * 
     * @param date 삭제할 날짜
     * @throws IllegalArgumentException date가 null인 경우
     * @throws RuntimeException 스토리지 삭제 중 오류가 발생한 경우
     */
    void delete(LocalDate date);

    ArchivingStrategy.ArchiveFormat getArchiveFormat();
}