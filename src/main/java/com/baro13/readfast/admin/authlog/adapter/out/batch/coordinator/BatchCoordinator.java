package com.baro13.readfast.admin.authlog.adapter.out.batch.coordinator;

import com.baro13.readfast.admin.authlog.adapter.out.batch.processor.ChunkProcessor;
import com.baro13.readfast.admin.authlog.adapter.out.batch.progress.BatchProgressTracker;
import com.baro13.readfast.admin.authlog.domain.port.AuthLogDbReader;
import com.baro13.readfast.admin.authlog.domain.port.BatchExecution;
import com.baro13.readfast.admin.policy.domain.model.DataRetentionPolicy;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 배치 실행 총괄 조정자
 * 전체 배치 실행 흐름을 관리하고 각 단계를 조정
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class BatchCoordinator implements BatchExecution {
    
    private final AuthLogDbReader authLogDbReader;
    private final ChunkProcessor chunkProcessor;
    private final BatchProgressTracker progressTracker;
    
    private static final int MAX_RECORDS_PER_BATCH = 100_000;
    private static final int DEFAULT_CHUNK_SIZE = 2000;
    private static final int BATCH_TIMEOUT_SECONDS = 3600;
    private static final int CHUNK_DELAY_MS = 50;

    @Override
    public BatchExecutionResult executeArchivingBatch(DataRetentionPolicy policy) {
        var startTime = LocalDateTime.now();
        
        log.info("아카이빙 배치 시작 - 정책: {}", policy.getPolicyId());
        
        try {
            var execution = new BatchExecution(policy, startTime);
            return executeBatchWithPolicy(execution);
            
        } catch (Exception e) {
            var endTime = LocalDateTime.now();
            var result = BatchExecutionResult.failure("배치 실행 실패: " + e.getMessage(), startTime, endTime);
            
            log.error("아카이빙 배치 실행 실패", e);
            return result;
        }
    }
    
    private BatchExecutionResult executeBatchWithPolicy(BatchExecution execution) {
        var cutoffDate = calculateCutoffDate(execution.policy());
        var chunkSize = calculateOptimalChunkSize(execution.policy());
        
        log.info("배치 설정 - 기준날짜: {}, 청크크기: {}", cutoffDate, chunkSize);
        
        // 배치 진행 상태 추적 시작
        progressTracker.startBatch(execution.policy());
        
        try {
            Long lastProcessedId = null;
            var batchTimeoutTime = execution.startTime().plusSeconds(BATCH_TIMEOUT_SECONDS);
            
            while (shouldContinueProcessing(execution, batchTimeoutTime)) {
                
                // 동시성 안전성을 위한 스냅샷 기반 데이터 조회
                var chunkData = authLogDbReader.findOlderThanWithSnapshot(
                    cutoffDate, chunkSize, lastProcessedId);
                
                if (chunkData.isEmpty()) {
                    log.info("처리할 데이터가 없어 배치 종료");
                    break;
                }
                
                // 청크 처리
                var chunkResult = chunkProcessor.processChunk(
                    chunkData, execution.policy(), progressTracker.nextChunkNumber());
                
                progressTracker.recordChunkResult(chunkResult);
                
                // 다음 청크를 위한 커서 업데이트
                lastProcessedId = chunkData.get(chunkData.size() - 1).getId();
                
                // 시스템 부하 방지를 위한 대기
                if (progressTracker.getProcessedChunkCount() > 1) {
                    waitBetweenChunks();
                }
            }
            
            var endTime = LocalDateTime.now();
            var result = progressTracker.buildSuccessResult(execution.startTime(), endTime);
            
            logBatchCompletion(result, execution.policy());
            return result;
            
        } finally {
            progressTracker.completeBatch();
        }
    }
    
    private LocalDateTime calculateCutoffDate(DataRetentionPolicy policy) {
        return LocalDateTime.now().minusDays(policy.getRetentionRule().getDbRetentionDays());
    }
    
    private int calculateOptimalChunkSize(DataRetentionPolicy policy) {
        var configuredSize = policy.getRetentionRule().getBatchSize();
        return Math.max(configuredSize, DEFAULT_CHUNK_SIZE);
    }
    
    private boolean shouldContinueProcessing(BatchExecution execution, LocalDateTime timeoutTime) {
        return progressTracker.getTotalProcessedRecords() < MAX_RECORDS_PER_BATCH &&
               LocalDateTime.now().isBefore(timeoutTime);
    }
    
    private void waitBetweenChunks() {
        try {
            Thread.sleep(CHUNK_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("배치 처리 중 인터럽트 발생");
            throw new RuntimeException("배치 인터럽트", e);
        }
    }
    
    private void logBatchCompletion(BatchExecutionResult result, DataRetentionPolicy policy) {
        var compressionType = policy.getArchivingStrategy().getCompressionType();
        var storageFormat = policy.getArchivingStrategy().getArchiveFormat();
        
        log.info("아카이빙 배치 완료 - 청크: {}, Processed: {}, Archived: {}, Deleted: {}, " +
                "실행시간: {}ms, 처리량: {:.2f} records/sec, 스토리지: {}, 압축: {}", 
                progressTracker.getProcessedChunkCount(),
                result.processedRecords(),
                result.archivedRecords(), 
                result.deletedRecords(),
                result.durationMillis(),
                calculateThroughput(result),
                storageFormat, 
                compressionType);
    }
    
    private double calculateThroughput(BatchExecutionResult result) {
        return result.durationMillis() > 0 ? 
               (result.processedRecords() * 1000.0 / result.durationMillis()) : 0;
    }
    
    /**
     * 배치 실행 컨텍스트
     */
    private record BatchExecution(DataRetentionPolicy policy, LocalDateTime startTime) {}
}