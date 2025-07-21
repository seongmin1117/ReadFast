package com.baro13.readfast.infrastructure.batch.facade;

import com.baro13.readfast.infrastructure.policy.DataRetentionProperties;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

/**
 * 배치 실행을 위한 Facade 클래스
 * 스케줄러와 배치 실행 로직 사이의 복잡성을 숨김
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchExecutionFacade {
    
    private final JobLauncher jobLauncher;
    private final Job dataArchivingJob;
    private final DataRetentionProperties properties;
    
    /**
     * 데이터 아카이빙 배치 실행
     */
    public BatchExecutionResult executeDataArchiving() {
        if (!properties.isEnableArchiving()) {
            log.info("데이터 아카이빙이 비활성화되어 있습니다");
            return BatchExecutionResult.skipped("아카이빙이 비활성화됨");
        }
        
        try {
            log.info("데이터 아카이빙 작업을 시작합니다...");
            
            JobParameters jobParameters = createJobParameters();
            JobExecution jobExecution = jobLauncher.run(dataArchivingJob, jobParameters);
            
            if (jobExecution.getStatus().isUnsuccessful()) {
                log.error("데이터 아카이빙 작업 실패: {}", jobExecution.getExitStatus());
                return BatchExecutionResult.failed(jobExecution.getExitStatus().getExitDescription());
            }
            
            log.info("데이터 아카이빙 작업이 성공적으로 완료되었습니다");
            return BatchExecutionResult.success(jobExecution.getId());
            
        } catch (Exception e) {
            log.error("데이터 아카이빙 작업 실행 실패", e);
            return BatchExecutionResult.failed(e.getMessage());
        }
    }
    
    /**
     * Job 파라미터 생성
     */
    private JobParameters createJobParameters() {
        return new JobParametersBuilder()
                .addString("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")))
                .addLong("dbRetentionDays", (long) properties.getDbRetentionDays())
                .addLong("batchSize", (long) properties.getBatchSize())
                .addString("executionMode", "scheduled")
                .toJobParameters();
    }
    
    /**
     * 수동 실행용 Job 파라미터 생성
     */
    private JobParameters createManualJobParameters() {
        return new JobParametersBuilder()
                .addString("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")))
                .addLong("dbRetentionDays", (long) properties.getDbRetentionDays())
                .addLong("batchSize", (long) properties.getBatchSize())
                .addString("executionMode", "manual")
                .toJobParameters();
    }
    
    /**
     * 수동 실행
     */
    public BatchExecutionResult executeManualArchiving() {
        try {
            log.info("수동 데이터 아카이빙이 실행되었습니다");
            
            JobParameters jobParameters = createManualJobParameters();
            JobExecution jobExecution = jobLauncher.run(dataArchivingJob, jobParameters);
            
            if (jobExecution.getStatus().isUnsuccessful()) {
                log.error("수동 데이터 아카이빙 작업 실패: {}", jobExecution.getExitStatus());
                return BatchExecutionResult.failed(jobExecution.getExitStatus().getExitDescription());
            }
            
            log.info("수동 데이터 아카이빙 작업이 성공적으로 완료되었습니다");
            return BatchExecutionResult.success(jobExecution.getId());
            
        } catch (Exception e) {
            log.error("수동 데이터 아카이빙 작업 실행 실패", e);
            return BatchExecutionResult.failed(e.getMessage());
        }
    }
    
    /**
     * 배치 실행 결과를 나타내는 클래스
     */
    public static class BatchExecutionResult {
        private final boolean success;
        private final String message;
        private final Long jobExecutionId;
        
        private BatchExecutionResult(boolean success, String message, Long jobExecutionId) {
            this.success = success;
            this.message = message;
            this.jobExecutionId = jobExecutionId;
        }
        
        public static BatchExecutionResult success(Long jobExecutionId) {
            return new BatchExecutionResult(true, "성공", jobExecutionId);
        }
        
        public static BatchExecutionResult failed(String message) {
            return new BatchExecutionResult(false, message, null);
        }
        
        public static BatchExecutionResult skipped(String message) {
            return new BatchExecutionResult(false, message, null);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Long getJobExecutionId() {
            return jobExecutionId;
        }
    }
}