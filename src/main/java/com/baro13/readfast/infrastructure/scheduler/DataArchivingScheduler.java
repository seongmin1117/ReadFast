package com.baro13.readfast.infrastructure.scheduler;

import com.baro13.readfast.infrastructure.batch.config.DataRetentionProperties;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "data.retention.enable-archiving", havingValue = "true", matchIfMissing = true)
public class DataArchivingScheduler {
    
    private final JobLauncher jobLauncher;
    private final Job dataArchivingJob;
    private final DataRetentionProperties properties;
    
    @Scheduled(cron = "#{@dataRetentionProperties.cronExpression}")
    public void scheduleDataArchiving() {
        if (!properties.isEnableArchiving()) {
            log.info("데이터 아카이빙이 비활성화되어 있습니다");
            return;
        }
        
        try {
            log.info("데이터 아카이빙 작업을 시작합니다...");
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")))
                    .addLong("dbRetentionDays", (long) properties.getDbRetentionDays())
                    .addLong("batchSize", (long) properties.getBatchSize())
                    .toJobParameters();
            
            jobLauncher.run(dataArchivingJob, jobParameters);
            
            log.info("데이터 아카이빙 작업이 성공적으로 완료되었습니다");
        } catch (Exception e) {
            log.error("데이터 아카이빙 작업 실행 실패", e);
        }
    }
    
    // 수동 실행을 위한 메서드 (필요시)
    public void executeManualArchiving() {
        log.info("수동 데이터 아카이빙이 실행되었습니다");
        scheduleDataArchiving();
    }
}