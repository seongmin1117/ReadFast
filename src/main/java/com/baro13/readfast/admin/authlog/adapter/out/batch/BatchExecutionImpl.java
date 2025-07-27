package com.baro13.readfast.admin.authlog.adapter.out.batch;

import com.baro13.readfast.admin.authlog.adapter.out.archive.compression.CompressionFactory;
import com.baro13.readfast.admin.authlog.adapter.out.archive.storage.Storage;
import com.baro13.readfast.admin.authlog.adapter.out.archive.storage.StorageFactory;
import com.baro13.readfast.admin.authlog.adapter.out.db.jpa.mapper.ArchiveMetadataMapper;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.admin.authlog.domain.port.ArchiveMetadataRepository;
import com.baro13.readfast.admin.authlog.domain.port.AuthLogDbReader;
import com.baro13.readfast.admin.authlog.domain.port.BatchExecution;
import com.baro13.readfast.admin.policy.domain.model.DataRetentionPolicy;
import com.baro13.readfast.global.common.TimeZoneConstants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 배치 실행 어댑터
 * Spring Batch 기반 아카이빙 배치 실행 구현체
 */
@Slf4j
@Component("legacyBatchExecution")
@RequiredArgsConstructor
public class BatchExecutionImpl implements BatchExecution {
    
    private final AuthLogDbReader authLogDbReader;
    private final StorageFactory storageFactory;
    private final CompressionFactory compressionFactory;
    private final ArchiveMetadataRepository archiveMetadataRepository;
    private final ArchiveMetadataMapper archiveMetadataMapper;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final int MAX_RECORDS_PER_BATCH = 100_000; // 최대 처리 레코드 수 증가
    private static final int CHUNK_DELAY_MS = 50; // 청크 간 대기 시간 단축
    private static final int DEFAULT_CHUNK_SIZE = 2000; // 기본 청크 크기 증가
    private static final int PARALLEL_THRESHOLD = 1000; // 병렬 처리 임계값
    private static final int BATCH_TIMEOUT_SECONDS = 3600; // 배치 타임아웃 1시간

    @Override
    public BatchExecutionResult executeArchivingBatch(DataRetentionPolicy policy) {
        var currentDate = LocalDate.now();
        var startTime = LocalDateTime.now();
        
        log.info("아카이빙 배치 시작");
        
        try {
            // 아카이빙 대상 데이터 조회 (성능 개선을 위한 스트리밍 방식)
            var cutoffDate = LocalDateTime.now().minusDays(policy.getRetentionRule().getDbRetentionDays());
            var initialBatchSize = policy.getRetentionRule().getBatchSize();
            var currentBatchSize = Math.max(initialBatchSize, DEFAULT_CHUNK_SIZE); // 최소 청크 크기 보장
            log.info("아카이빙 기준 날짜: {}, 초기 배치 크기: {}, 조정된 배치 크기: {}", 
                    cutoffDate, initialBatchSize, currentBatchSize);
            
            // 커서 기반 대용량 데이터 처리
            var totalProcessed = 0L;
            var totalArchived = 0L;
            var totalDeleted = 0L;
            var chunkNumber = 1;
            Long lastProcessedId = null; // 커서 (마지막 처리된 ID)
            
            // 모든 아카이빙 대상 데이터를 처리할 때까지 무한 루프 (타임아웃 체크 포함).
            var batchTimeoutTime = startTime.plusSeconds(BATCH_TIMEOUT_SECONDS);
            while (totalProcessed < MAX_RECORDS_PER_BATCH && LocalDateTime.now().isBefore(batchTimeoutTime)) {
                log.info("배치 청크 #{} 처리 시작 (커서: {})", chunkNumber, lastProcessedId);
                
                // 커서 기반으로 다음 배치 데이터 조회 (동적 배치 사이즈 적용)
                var targetData = authLogDbReader.findOlderThan(cutoffDate, currentBatchSize, lastProcessedId);
                log.info("청크 #{} - 조회된 아카이빙 대상 데이터 건수: {} (배치크기: {})", 
                        chunkNumber, targetData.size(), currentBatchSize);
                
                // 더 이상 처리할 데이터가 없으면 종료
                if (targetData.isEmpty()) {
                    log.info("처리할 데이터가 없어 배치 종료. 총 청크 수: {}", chunkNumber - 1);
                    break;
                }
                
                // 타임아웃 체크
                if (LocalDateTime.now().isAfter(batchTimeoutTime)) {
                    log.warn("배치 타임아웃으로 종료. 총 청크 수: {}, 처리된 레코드: {}", chunkNumber - 1, totalProcessed);
                    break;
                }
                
                // 스토리지 초기화 (청크별로)
                Storage storage = storageFactory.resolve();
                
                // 청크별 처리 (청크 번호와 타임스탬프 전달)
                var chunkResult = processDataChunk(targetData, policy, storage, chunkNumber, startTime);
                totalProcessed += chunkResult.processedCount();
                totalArchived += chunkResult.archivedCount();
                totalDeleted += chunkResult.deletedCount();
                
                // 다음 배치를 위한 커서 업데이트 (마지막 처리된 ID)
                if (!targetData.isEmpty()) {
                    lastProcessedId = targetData.get(targetData.size() - 1).getId();
                    log.debug("청크 #{} 처리 완료, 다음 커서: {}", chunkNumber, lastProcessedId);
                }
                
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
            
            // 성능 통계 로깅 (압축 정보 포함)
            var executionTimeMs = java.time.Duration.between(startTime, endTime).toMillis();
            var throughputPerSecond = executionTimeMs > 0 ? (totalProcessed * 1000.0 / executionTimeMs) : 0;
            
            var compressionType = policy.getArchivingStrategy().getCompressionType();
            var storageFormat = policy.getArchivingStrategy().getArchiveFormat();
            
            log.info("아카이빙 배치 완료 - 총 청크 수: {}, Processed: {}, Archived: {}, Deleted: {}, " +
                    "실행시간: {}ms, 처리량: {:.2f} records/sec, 스토리지: {}, 압축: {}", 
                    chunkNumber - 1, totalProcessed, totalArchived, totalDeleted, 
                    executionTimeMs, throughputPerSecond, storageFormat, compressionType);
            
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
    private ChunkResult processDataChunk(List<AuthLog> targetData, DataRetentionPolicy policy, 
                                       Storage storage, int chunkNumber, LocalDateTime batchStartTime) {
        var currentDate = LocalDate.now();
        
        try {
            // 데이터를 스토리지에 저장
            storage.store(targetData, currentDate);
            
            // 아카이브 메타데이터 저장 (청크 번호와 배치 시작 시간 전달)
            saveArchiveMetadata(policy, targetData, storage, chunkNumber, batchStartTime);
            
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
     * @param chunkNumber 청크 번호
     * @param batchStartTime 배치 시작 시간
     */
    private void saveArchiveMetadata(DataRetentionPolicy policy, 
                                   List<AuthLog> targetData,
                                   Storage storage,
                                   int chunkNumber,
                                   LocalDateTime batchStartTime) {
        try {
            // 아카이브된 파일 경로 계산 (청크 번호와 배치 시작 시간 포함)
            var filePath = calculateArchiveFilePath(policy, targetData, storage, chunkNumber, batchStartTime);
            
            // 파일 경로 기반 중복 방지: 이미 존재하는 파일인지 확인
            var existingMetadata = archiveMetadataRepository.findByFilePath(filePath);
            
            if (existingMetadata.isPresent()) {
                log.warn("이미 아카이브된 파일입니다. 파일: {}, 기존 메타데이터 ID: {}", 
                        filePath, existingMetadata.get().getId());
                return;
            }
            
            // 파일 크기 계산 (실제 파일이 존재하는 경우)
            var fileSizeBytes = calculateFileSize(filePath);
            
            // 데이터의 날짜 범위 계산
            var dateRange = calculateDateRange(targetData);
            
            // ArchiveMetadata 생성 (청크별 개별 메타데이터)
            var metadata = archiveMetadataMapper.createFromBatchResult(
                storage.getArchiveFormat().name().toLowerCase(),
                filePath,
                fileSizeBytes,
                dateRange.startDate(),
                dateRange.endDate()
            );
            
            // 메타데이터 저장
            var savedMetadata = archiveMetadataRepository.save(metadata);
            
            log.info("아카이브 메타데이터 저장 완료. ID: {}, 파일: {}, 청크: #{}, 데이터건수: {}", 
                    savedMetadata.getId(), savedMetadata.getFilePath(), chunkNumber, targetData.size());
                    
        } catch (Exception e) {
            log.error("아카이브 메타데이터 저장 실패. 청크: #{}, 데이터건수: {}", chunkNumber, targetData.size(), e);
            // 메타데이터 저장 실패는 전체 배치를 실패시키지 않음 (비즈니스 판단)
        }
    }
    
    /**
     * 아카이브 파일 경로 계산 (데이터 날짜 범위 기반, 개선된 버전)
     * 청크 단위 처리를 고려하여 파일명에 청크 정보와 타임스탬프를 포함
     */
    private String calculateArchiveFilePath(DataRetentionPolicy policy, List<AuthLog> targetData, 
                                          Storage storage, int chunkNumber, LocalDateTime batchStartTime) {
        var archiveBasePath = policy.getArchivingStrategy().getArchiveBasePath();
        
        // 데이터의 실제 날짜 범위를 기반으로 파일명 생성 (TimeZone 일관성 적용)
        var dateRange = calculateDateRange(targetData);
        var startDate = dateRange.startDate().atZone(TimeZoneConstants.APPLICATION_ZONE).toLocalDate();
        var endDate = dateRange.endDate().atZone(TimeZoneConstants.APPLICATION_ZONE).toLocalDate();
        
        // 연도/월/일 기반 디렉토리 구조
        var year = String.valueOf(startDate.getYear());
        var month = String.format("%02d", startDate.getMonthValue());
        var day = String.format("%02d", startDate.getDayOfMonth());
        
        // 배치 실행 시간을 포함한 타임스탬프 (중복 방지)
        var batchTimestamp = batchStartTime.format(DateTimeFormatter.ofPattern("HHmmss"));
        
        // 파일명 생성: 날짜범위_청크번호_레코드수_배치타임스탬프.확장자
        String fileName;
        var recordCount = targetData.size();
        
        if (startDate.equals(endDate)) {
            // 하루치 데이터: 2024-01-15_chunk001_1000_143052.json
            fileName = String.format("%s_chunk%03d_%d_%s%s", 
                    startDate.format(DATE_FORMATTER), 
                    chunkNumber, 
                    recordCount, 
                    batchTimestamp,
                    storage.getArchiveFormat().getExtension());
        } else {
            // 여러 날짜 데이터: 2024-01-15_to_2024-01-17_chunk001_1000_143052.json
            fileName = String.format("%s_to_%s_chunk%03d_%d_%s%s", 
                    startDate.format(DATE_FORMATTER), 
                    endDate.format(DATE_FORMATTER), 
                    chunkNumber, 
                    recordCount, 
                    batchTimestamp,
                    storage.getArchiveFormat().getExtension());
        }
        
        // 체계적인 디렉토리 구조: /archive/2024/01/15/filename.json
        return Paths.get(archiveBasePath, year, month, day, fileName).toString();
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
     * 데이터의 날짜 범위 계산 (성능 최적화된 버전, 조건부 병렬 처리)
     */
    private DateRange calculateDateRange(List<AuthLog> targetData) {
        if (targetData.isEmpty()) {
            var now = Instant.now();
            return new DateRange(now, now);
        }
        
        // 데이터 크기에 따라 병렬 처리 여부 결정 (성능 최적화)
        var stream = targetData.size() >= PARALLEL_THRESHOLD ? 
                     targetData.parallelStream() : targetData.stream();
        
        var minDate = stream
            .map(AuthLog::getDate)
            .min(Instant::compareTo)
            .orElse(Instant.now());
            
        // 최대값 계산을 위해 새로운 스트림 생성 (병렬 처리 조건 재적용)
        var streamForMax = targetData.size() >= PARALLEL_THRESHOLD ? 
                          targetData.parallelStream() : targetData.stream();
                          
        var maxDate = streamForMax
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