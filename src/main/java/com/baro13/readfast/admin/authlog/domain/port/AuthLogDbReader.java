package com.baro13.readfast.admin.authlog.domain.port;

import com.baro13.readfast.admin.authlog.adapter.in.controller.dto.AuthSearchCondition;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuthLogDbReader {
    /**
     * 커서 기반 성능 최적화된 인증 로그 검색
     * DB에서 인증 로그를 조회하는 최적화된 방법
     */
    Page<AuthLog> search(AuthSearchCondition condition, Pageable pageable);
    
    /**
     * 커서 기반으로 지정된 날짜보다 오래된 인증 로그 조회 (아카이빙용)
     * ID 기반 커서를 사용하여 중복 없이 순차적으로 데이터를 처리
     * 
     * @param cutoffDate 기준 날짜
     * @param batchSize 배치 크기
     * @param lastProcessedId 마지막 처리된 ID (커서), null이면 처음부터 시작
     * @return 조회된 인증 로그 리스트 (ID 순으로 정렬)
     */
    List<AuthLog> findOlderThan(LocalDateTime cutoffDate, int batchSize, Long lastProcessedId);
    
    /**
     * 동시성 안전성을 위한 스냅샷 기반 조회
     * 배치 처리 중 데이터 삽입/수정이 있어도 일관된 결과 보장
     * 
     * @param cutoffDate 기준 날짜
     * @param limit 조회 제한
     * @param lastProcessedId 마지막 처리된 ID (커서)
     * @return 스냅샷 기반 조회 결과
     */
    List<AuthLog> findOlderThanWithSnapshot(LocalDateTime cutoffDate, int limit, Long lastProcessedId);
    
    /**
     * 특정 ID 목록의 데이터 삭제
     * 
     * @param ids 삭제할 ID 목록
     * @return 삭제된 레코드 수
     */
    int deleteByIds(List<Long> ids);
    
    /**
     * 지정된 날짜보다 오래된 데이터 개수 조회
     * 
     * @param cutoffDate 기준 날짜
     * @return 조회된 레코드 수
     */
    long countOlderThan(LocalDateTime cutoffDate);
    
    /**
     * 특정 날짜 범위의 인증 로그 조회 (날짜별 아카이빙용)
     * 
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param batchSize 배치 크기
     * @param lastProcessedId 마지막 처리된 ID (커서)
     * @return 조회된 인증 로그 리스트
     */
    List<AuthLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, int batchSize, Long lastProcessedId);
}
