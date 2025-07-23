package com.baro13.readfast.admin.authlog.adapter.in.scheduler;

import com.baro13.readfast.admin.authlog.application.in.ArchivingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 아카이빙 스케줄러 (인바운드 어댑터)
 * 매일 새벽 2시에 아카이빙 배치 작업 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArchivingScheduler {

    private final ArchivingService archivingService;

    /**
     * 매일 새벽 2시에 아카이빙 배치 실행
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void executeScheduledArchiving() {
        log.info("스케줄된 아카이빙 배치 시작");
        
        try {
            var result = archivingService.executeArchivingBatch();
            
            if (result.success()) {
                log.info("스케줄된 아카이빙 배치 성공 완료. 처리건수: {}, 실행시간: {}ms", 
                        result.processedRecords(), result.durationMillis());
            } else {
                log.error("스케줄된 아카이빙 배치 실패: {}", result.message());
            }
            
        } catch (Exception e) {
            log.error("스케줄된 아카이빙 배치 실행 중 예외 발생", e);
        }
    }
}