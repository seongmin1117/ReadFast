package com.baro13.readfast.admin.authlog.adapter.out.batch;

import com.baro13.readfast.admin.authlog.adapter.out.archive.DataStorageFactory;
import com.baro13.readfast.admin.authlog.adapter.out.archive.storage.DataStorage;
import com.baro13.readfast.admin.authlog.adapter.out.db.jpa.mapper.ArchiveMetadataMapper;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.admin.authlog.domain.port.ArchiveMetadataRepository;
import com.baro13.readfast.admin.authlog.domain.port.AuthLogDbReader;
import com.baro13.readfast.admin.authlog.domain.port.BatchExecution;
import com.baro13.readfast.admin.policy.domain.model.DataRetentionPolicy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.baro13.readfast.global.common.TimeZoneConstants;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 배치 실행 어댑터
 * Spring Batch 기반 아카이빙 배치 실행 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchExecutionImpl implements BatchExecution {
    
    private final AuthLogDbReader authLogDbReader;
    private final DataStorageFactory dataStorageFactory;
    private final ArchiveMetadataRepository archiveMetadataRepository;
    private final ArchiveMetadataMapper archiveMetadataMapper;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // Configuration constants
    private static final int MAX_RECORDS_PER_BATCH = 50_000;
    private static final int CHUNK_DELAY_MS = 100;
    private static final int DEFAULT_CHUNK_SIZE = 1000;

    @Override
    public BatchExecutionResult executeArchivingBatch(DataRetentionPolicy policy) {
        var currentDate = LocalDate.now();
        var startTime = LocalDateTime.now();
        
        log.info("아카이빙 배치 시작");
        
        try {
            // 아카이빙 대상 데이터 조회 (성능 개선을 위한 스트리밍 방식)
            var cutoffDate = LocalDateTime.now().minusDays(policy.getRetentionRule().getDbRetentionDays());
            var batchSize = policy.getRetentionRule().getBatchSize();
            log.info("아카이빙 기준 날짜: {}, 배치 크기: {}", cutoffDate, batchSize);
            
            // 대용량 데이터 처리를 위한 청크 단위 처리
            var totalProcessed = 0L;
            var totalArchived = 0L;
            var totalDeleted = 0L;
            var hasMoreData = true;
            var chunkNumber = 1;
            
            while (hasMoreData && totalProcessed < MAX_RECORDS_PER_BATCH) {
                log.info("배치 청크 #{} 처리 시작", chunkNumber);
                
                var targetData = authLogDbReader.findOlderThan(cutoffDate, batchSize);
                log.info("청크 #{} - 조회된 아카이빙 대상 데이터 건수: {}", chunkNumber, targetData.size());
                
                if (targetData.isEmpty()) {
                    hasMoreData = false;
                    break;
                }
                
                // 스토리지 초기화 (청크별로)
                DataStorage storage = dataStorageFactory.resolve();
                
                // 청크별 처리
                var chunkResult = processDataChunk(targetData, policy, storage, chunkNumber);
                totalProcessed += chunkResult.processedCount();
                totalArchived += chunkResult.archivedCount();
                totalDeleted += chunkResult.deletedCount();
                
                chunkNumber++;
                
                // 청크 사이 잠시 대기 (시스템 부하 방지)
                if (chunkNumber > 2) { // 첫 번째 청크는 대기하지 않음
                    try {
                        Thread.sleep(CHUNK_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("배치 처리 중 인터럽트 발생");
                        break;
                    }
                }
            }
            
            var endTime = LocalDateTime.now();
            var successResult = BatchExecutionResult.success(
                totalProcessed, 
                totalArchived, 
                totalDeleted,
                java.time.Duration.between(startTime, endTime).toMillis(),
                startTime, 
                endTime
            );
            
            log.info("아카이빙 배치 완료 - 총 청크 수: {}, Processed: {}, Archived: {}, Deleted: {}", 
                    chunkNumber - 1, totalProcessed, totalArchived, totalDeleted);
            
            return successResult;
            
        } catch (Exception e) {
            var endTime = LocalDateTime.now();
            var failureResult = BatchExecutionResult.failure("배치 실행 실패: " + e.getMessage(), startTime, endTime);
            
            log.error("아카이빙 배치 실행 실패", e);
            return failureResult;
        }
    }

    /**
     * 데이터 청크 처리 (성능 최적화)
     */
    private ChunkResult processDataChunk(List<AuthLog> targetData, DataRetentionPolicy policy, DataStorage storage, int chunkNumber) {
        var currentDate = LocalDate.now();
        
        try {
            // 데이터를 스토리지에 저장
            storage.store(targetData, currentDate);
            
            // 아카이브 메타데이터 저장
            saveArchiveMetadata(policy, targetData, storage);
            
            // 데이터 삭제 (정책에 따라)
            var deletedCount = 0L;
            if (policy.getRetentionRule().isEnableDataDeletion()) {
                // MVP: 실제 삭제는 구현하지 않음 (데이터 안전을 위해)
                deletedCount = targetData.size();
                log.debug("청크 #{} - 데이터 삭제 시뮬레이션: {}건", chunkNumber, deletedCount);
            }
            
            log.info("청크 #{} 처리 완료 - Processed: {}, Archived: {}, Deleted: {}", 
                    chunkNumber, targetData.size(), targetData.size(), deletedCount);
            
            return new ChunkResult(targetData.size(), targetData.size(), deletedCount);
            
        } catch (Exception e) {
            log.error("청크 #{} 처리 실패. 데이터 건수: {}", chunkNumber, targetData.size(), e);
            return new ChunkResult(targetData.size(), 0, 0);
        }
    }

    /**
     * 청크 처리 결과
     */
    private record ChunkResult(long processedCount, long archivedCount, long deletedCount) {}
    
    /**
     * 아카이브 메타데이터를 데이터베이스에 저장
     * 
     * @param policy 데이터 보관 정책
     * @param targetData 아카이브된 데이터
     * @param storage 사용된 스토리지
     */
    private void saveArchiveMetadata(DataRetentionPolicy policy, 
                                   List<AuthLog> targetData,
                                   DataStorage storage) {
        try {
            // 중복 방지: 이미 존재하는 메타데이터인지 확인
            var dateRange = calculateDateRange(targetData);
            var existingMetadata = archiveMetadataRepository.findByExactDateRange(
                dateRange.startDate(), 
                dateRange.endDate()
            );
            
            if (!existingMetadata.isEmpty()) {
                log.warn("이미 아카이브된 데이터 범위입니다. 기간: {} ~ {}, 기존 메타데이터 수: {}", 
                        dateRange.startDate(), dateRange.endDate(), existingMetadata.size());
                return;
            }
            
            // 아카이브된 파일 경로 계산
            var filePath = calculateArchiveFilePath(policy, targetData, storage);
            
            // 파일 크기 계산 (실제 파일이 존재하는 경우)
            var fileSizeBytes = calculateFileSize(filePath);
            
            // ArchiveMetadata 생성
            var metadata = archiveMetadataMapper.createFromBatchResult(
                storage.getArchiveFormat().name().toLowerCase(),
                filePath,
                fileSizeBytes,
                dateRange.startDate(),
                dateRange.endDate()
            );
            
            // 메타데이터 저장
            var savedMetadata = archiveMetadataRepository.save(metadata);
            
            log.info("아카이브 메타데이터 저장 완료. ID: {}, 파일: {}, 데이터건수: {}", 
                    savedMetadata.getId(), savedMetadata.getFilePath(), targetData.size());
                    
        } catch (Exception e) {
            log.error("아카이브 메타데이터 저장 실패. 데이터건수: {}", targetData.size(), e);
            // 메타데이터 저장 실패는 전체 배치를 실패시키지 않음 (비즈니스 판단)
        }
    }
    
    /**
     * 아카이브 파일 경로 계산 (데이터 날짜 범위 기반)
     */
    private String calculateArchiveFilePath(DataRetentionPolicy policy, List<AuthLog> targetData, DataStorage storage) {
        var archiveBasePath = policy.getArchivingStrategy().getArchiveBasePath();
        
        // 데이터의 실제 날짜 범위를 기반으로 파일명 생성 (TimeZone 일관성 적용)
        var dateRange = calculateDateRange(targetData);
        var startDateStr = dateRange.startDate().atZone(TimeZoneConstants.APPLICATION_ZONE).toLocalDate().format(DATE_FORMATTER);
        var endDateStr = dateRange.endDate().atZone(TimeZoneConstants.APPLICATION_ZONE).toLocalDate().format(DATE_FORMATTER);
        
        // 날짜 범위가 같은 경우 (하루치 데이터)
        String fileName;
        if (startDateStr.equals(endDateStr)) {
            fileName = startDateStr + storage.getArchiveFormat().getExtension();
        } else {
            fileName = startDateStr + "_to_" + endDateStr + storage.getArchiveFormat().getExtension();
        }
        
        // 시작 날짜를 기준으로 디렉토리 구성
        return Paths.get(archiveBasePath, startDateStr, fileName).toString();
    }
    
    /**
     * 파일 크기 계산 (실제 파일이 존재하는 경우)
     */
    private Long calculateFileSize(String filePath) {
        try {
            var path = Paths.get(filePath);
            if (Files.exists(path)) {
                return Files.size(path);
            }
        } catch (IOException e) {
            log.warn("파일 크기 계산 실패. 파일: {}", filePath, e);
        }
        return null; // 파일이 없거나 크기를 알 수 없는 경우
    }
    
    /**
     * 데이터의 날짜 범위 계산 (TimeZone 일관성 적용)
     */
    private DateRange calculateDateRange(List<AuthLog> targetData) {
        if (targetData.isEmpty()) {
            var now = Instant.now();
            return new DateRange(now, now);
        }
        
        var minDate = targetData.parallelStream()
            .map(AuthLog::getDate)
            .min(Instant::compareTo)
            .orElse(Instant.now());
            
        var maxDate = targetData.parallelStream()
            .map(AuthLog::getDate)
            .max(Instant::compareTo)
            .orElse(Instant.now());
            
        return new DateRange(minDate, maxDate);
    }
    
    /**
     * 날짜 범위를 나타내는 레코드
     */
    private record DateRange(Instant startDate, Instant endDate) {}

}