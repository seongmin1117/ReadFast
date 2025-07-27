package com.baro13.readfast.admin.authlog.adapter.out.batch.progress;

import com.baro13.readfast.admin.authlog.adapter.out.batch.processor.ChunkResult;
import com.baro13.readfast.admin.authlog.domain.port.BatchExecution.BatchExecutionResult;
import com.baro13.readfast.admin.policy.domain.model.DataRetentionPolicy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 배치 진행 상태 추적자
 * 배치 실행 중 진행률과 성과를 실시간으로 추적
 */
@Slf4j
@Component
public class BatchProgressTracker {
    
    private final AtomicInteger processedChunkCount = new AtomicInteger(0);
    private final AtomicLong totalProcessedRecords = new AtomicLong(0);
    private final AtomicLong totalArchivedRecords = new AtomicLong(0);
    private final AtomicLong totalDeletedRecords = new AtomicLong(0);
    
    private volatile DataRetentionPolicy currentPolicy;
    private volatile LocalDateTime batchStartTime;

    /**
     * 배치 시작
     */
    public void startBatch(DataRetentionPolicy policy) {
        this.currentPolicy = policy;
        this.batchStartTime = LocalDateTime.now();
        
        // 카운터 초기화
        processedChunkCount.set(0);
        totalProcessedRecords.set(0);
        totalArchivedRecords.set(0);
        totalDeletedRecords.set(0);
        
        log.info("배치 진행 추적 시작 - 정책 ID: {}", policy.getPolicyId());
    }
    
    /**
     * 청크 결과 기록
     */
    public void recordChunkResult(ChunkResult result) {
        var chunkNum = processedChunkCount.incrementAndGet();
        
        totalProcessedRecords.addAndGet(result.processedCount());
        totalArchivedRecords.addAndGet(result.archivedCount());
        totalDeletedRecords.addAndGet(result.deletedCount());
        
        // 진행률 로깅 (매 10청크마다)
        if (chunkNum % 10 == 0) {
            logProgressUpdate(chunkNum);
        }
    }
    
    /**
     * 다음 청크 번호 반환
     */
    public int nextChunkNumber() {
        return processedChunkCount.get() + 1;
    }
    
    /**
     * 처리된 청크 수 반환
     */
    public int getProcessedChunkCount() {
        return processedChunkCount.get();
    }
    
    /**
     * 총 처리된 레코드 수 반환
     */
    public long getTotalProcessedRecords() {
        return totalProcessedRecords.get();
    }
    
    /**
     * 성공 결과 생성
     */
    public BatchExecutionResult buildSuccessResult(LocalDateTime startTime, LocalDateTime endTime) {
        var executionTimeMs = Duration.between(startTime, endTime).toMillis();
        
        return BatchExecutionResult.success(
            totalProcessedRecords.get(),
            totalArchivedRecords.get(),
            totalDeletedRecords.get(),
            executionTimeMs,
            startTime,
            endTime
        );
    }
    
    /**
     * 배치 완료
     */
    public void completeBatch() {
        var endTime = LocalDateTime.now();
        var executionTime = Duration.between(batchStartTime, endTime);
        
        log.info("배치 진행 추적 완료 - 총 실행시간: {}ms, 총 청크: {}, 총 레코드: {}", 
                executionTime.toMillis(), 
                processedChunkCount.get(), 
                totalProcessedRecords.get());
        
        // 상태 초기화
        currentPolicy = null;
        batchStartTime = null;
    }
    
    /**
     * 현재 진행률 계산 (예상치 기반)
     */
    public double calculateProgress() {
        if (currentPolicy == null) {
            return 0.0;
        }
        
        // 간단한 진행률 계산 (실제로는 더 정교한 로직 필요)
        var estimatedTotal = currentPolicy.getRetentionRule().getBatchSize() * 50; // 추정치
        return Math.min(1.0, (double) totalProcessedRecords.get() / estimatedTotal);
    }
    
    /**
     * 현재 처리 속도 계산 (records/sec)
     */
    public double getCurrentThroughput() {
        if (batchStartTime == null) {
            return 0.0;
        }
        
        var elapsedSeconds = Duration.between(batchStartTime, LocalDateTime.now()).getSeconds();
        return elapsedSeconds > 0 ? (double) totalProcessedRecords.get() / elapsedSeconds : 0.0;
    }
    
    private void logProgressUpdate(int chunkNumber) {
        var progress = calculateProgress();
        var throughput = getCurrentThroughput();
        
        log.info("배치 진행 상황 - 청크: {}, 진행률: {:.1f}%, 처리량: {:.2f} records/sec, " +
                "누적 레코드: {}", 
                chunkNumber, 
                progress * 100, 
                throughput, 
                totalProcessedRecords.get());
    }
}