package com.baro13.readfast.infrastructure.scheduler;

import com.baro13.readfast.infrastructure.batch.facade.BatchExecutionFacade;
import com.baro13.readfast.infrastructure.policy.DataRetentionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 간소화된 데이터 아카이빙 스케줄러
 * 복잡한 배치 실행 로직은 Facade에 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "data.retention.enable-archiving", havingValue = "true", matchIfMissing = true)
public class SimpleDataArchivingScheduler {
    
    private final BatchExecutionFacade batchExecutionFacade;
    private final DataRetentionProperties properties;
    
    @Scheduled(cron = "#{@dataRetentionProperties.cronExpression}")
    public void scheduleDataArchiving() {
        log.info("스케줄된 데이터 아카이빙 시작 - 보관 기간: {}일", properties.getDbRetentionDays());
        
        BatchExecutionFacade.BatchExecutionResult result = batchExecutionFacade.executeDataArchiving();
        
        if (result.isSuccess()) {
            log.info("스케줄된 데이터 아카이빙 성공 - JobExecutionId: {}", result.getJobExecutionId());
        } else {
            log.error("스케줄된 데이터 아카이빙 실패: {}", result.getMessage());
        }
    }
    
    /**
     * 수동 실행을 위한 메서드
     */
    public BatchExecutionFacade.BatchExecutionResult executeManualArchiving() {
        log.info("수동 데이터 아카이빙 요청");
        
        BatchExecutionFacade.BatchExecutionResult result = batchExecutionFacade.executeManualArchiving();
        
        if (result.isSuccess()) {
            log.info("수동 데이터 아카이빙 성공 - JobExecutionId: {}", result.getJobExecutionId());
        } else {
            log.error("수동 데이터 아카이빙 실패: {}", result.getMessage());
        }
        
        return result;
    }
}