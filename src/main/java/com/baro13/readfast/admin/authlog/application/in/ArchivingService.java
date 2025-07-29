package com.baro13.readfast.admin.authlog.application.in;

import com.baro13.readfast.admin.authlog.domain.exception.ArchivingException;
import com.baro13.readfast.admin.authlog.domain.port.BatchExecution;
import com.baro13.readfast.admin.authlog.domain.port.BatchExecution.BatchExecutionResult;
import com.baro13.readfast.admin.authlog.domain.port.DataRetentionPolicyProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 배치 실행 서비스 구현체
 * 도메인 포트를 호출하여 배치 작업 실행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchivingService {

    private final BatchExecution batchExecution;
    private final DataRetentionPolicyProvider policyProvider;

    @Transactional(readOnly = false) // MASTER DB 사용 보장
    public BatchExecutionResult executeArchivingBatch() {
        log.info("아카이빙 배치 서비스 실행 시작");
        
        try {
            var policy = policyProvider.getCurrentPolicy();
            var result = batchExecution.executeArchivingBatch(policy);
            
            log.info("아카이빙 배치 서비스 실행 완료. 성공: {}, 처리건수: {}", 
                    result.success(), result.processedRecords());
            
            return result;
            
        } catch (Exception e) {
            log.error("아카이빙 배치 서비스 실행 중 예외 발생", e);
            throw new ArchivingException("아카이빙 배치 실행 실패", e);
        }
    }

    /**
     * 날짜 지정 아카이빙 배치 실행
     */
    @Transactional(readOnly = false)
    public BatchExecutionResult executeArchivingBatchByDate(LocalDate targetDate) {
        log.info("날짜 지정 아카이빙 배치 서비스 실행 시작: {}", targetDate);
        
        try {
            var policy = policyProvider.getCurrentPolicy();
            var result = batchExecution.executeArchivingBatchByDate(policy, targetDate);
            
            log.info("날짜 지정 아카이빙 배치 서비스 실행 완료. 대상날짜: {}, 성공: {}, 처리건수: {}", 
                    targetDate, result.success(), result.processedRecords());
            
            return result;
            
        } catch (Exception e) {
            log.error("날짜 지정 아카이빙 배치 서비스 실행 중 예외 발생. 대상날짜: {}", targetDate, e);
            throw new ArchivingException("날짜 지정 배치 실행 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 배치 아카이브 상태 조회 (시연용 구현)
     */
    public Map<String, Object> getArchivingStatus() {
        log.info("배치 아카이브 상태 조회");
        
        var status = new HashMap<String, Object>();
        
        try {
            var policy = policyProvider.getCurrentPolicy();
            
            // 기본 상태 정보
            status.put("isRunning", false); // 현재는 동기식이므로 항상 false
            status.put("lastExecutionTime", LocalDateTime.now().minusHours(1)); // 예시
            status.put("nextScheduledTime", LocalDateTime.now().plusHours(23)); // 예시
            
            // 정책 정보
            status.put("currentPolicy", Map.of(
                "policyId", policy.getPolicyId(),
                "dbRetentionDays", policy.getRetentionRule().getDbRetentionDays(),
                "totalRetentionDays", policy.getRetentionRule().getTotalRetentionDays(),
                "batchSize", policy.getRetentionRule().getBatchSize(),
                "enableDataDeletion", policy.getRetentionRule().isEnableDataDeletion(),
                "archiveBasePath", policy.getArchivingStrategy().getArchiveBasePath(),
                "archiveFormat", policy.getArchivingStrategy().getArchiveFormat().name(),
                "compressionType", policy.getArchivingStrategy().getCompressionType().name()
            ));
            
            // 실행 통계 (시연용 더미 데이터)
            status.put("statistics", Map.of(
                "totalExecutions", 15,
                "successfulExecutions", 14,
                "failedExecutions", 1,
                "averageProcessingTime", "2.5 minutes",
                "lastProcessedRecords", 1250
            ));
            
            log.info("배치 상태 조회 완료");
            return status;
            
        } catch (Exception e) {
            log.error("배치 상태 조회 중 예외 발생", e);
            status.put("error", "상태 조회 실패: " + e.getMessage());
            return status;
        }
    }
}