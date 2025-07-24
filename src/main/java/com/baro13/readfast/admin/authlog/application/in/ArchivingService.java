package com.baro13.readfast.admin.authlog.application.in;

import com.baro13.readfast.admin.authlog.domain.port.BatchExecution;
import com.baro13.readfast.admin.authlog.domain.port.BatchExecution.BatchExecutionResult;
import com.baro13.readfast.admin.authlog.domain.port.DataRetentionPolicyProvider;
import com.baro13.readfast.admin.authlog.domain.exception.ArchivingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
}