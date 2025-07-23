package com.baro13.readfast.admin.authlog.domain.port;

import com.baro13.readfast.admin.policy.domain.model.DataRetentionPolicy;
import java.time.LocalDateTime;

/**
 * 배치 실행 포트 (아웃바운드)
 * 헥사고날 아키텍처의 포트로서 배치 실행에 대한 추상화
 */
public interface BatchExecution {
    
    /**
     * 아카이빙 배치 실행
     */
    BatchExecutionResult executeArchivingBatch(DataRetentionPolicy policy);

    record BatchExecutionResult(
            boolean success,
            long processedRecords,
            long archivedRecords,
            long deletedRecords,
            long durationMillis,
            String message,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        public double getSuccessRate() {
            return processedRecords > 0 ? (double) archivedRecords / processedRecords * 100 : 0.0;
        }
        
        public static BatchExecutionResult success(long processedRecords,
                                                 long archivedRecords, long deletedRecords, 
                                                 long durationMillis, LocalDateTime startTime, 
                                                 LocalDateTime endTime) {
            return new BatchExecutionResult(true, processedRecords, archivedRecords, deletedRecords,
                    durationMillis, "배치 실행 성공", startTime, endTime
            );
        }
        
        public static BatchExecutionResult failure(String message,
                                                 LocalDateTime startTime, LocalDateTime endTime) {
            return new BatchExecutionResult(false, 0, 0, 0,
                    java.time.Duration.between(startTime, endTime).toMillis(), 
                    message, startTime, endTime
            );
        }
    }

}