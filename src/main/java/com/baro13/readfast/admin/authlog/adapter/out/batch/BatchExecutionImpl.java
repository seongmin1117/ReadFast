package com.baro13.readfast.admin.authlog.adapter.out.batch;

import com.baro13.readfast.admin.authlog.adapter.out.archive.storage.Storage;
import com.baro13.readfast.admin.authlog.adapter.out.archive.storage.StorageFactory;
import com.baro13.readfast.admin.authlog.adapter.out.db.jpa.mapper.ArchiveMetadataMapper;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.admin.authlog.domain.port.ArchiveMetadataRepository;
import com.baro13.readfast.admin.authlog.domain.port.AuthLogDbReader;
import com.baro13.readfast.admin.authlog.domain.port.BatchExecution;
import com.baro13.readfast.admin.policy.domain.model.DataRetentionPolicy;
import com.baro13.readfast.global.common.DateTimeUtils;
import com.baro13.readfast.global.datasource.RoutingDataSourceContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 단순화된 배치 실행 구현체
 * 일자별로 단일 파일 생성
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class BatchExecutionImpl implements BatchExecution {
    
    private final AuthLogDbReader authLogDbReader;
    private final StorageFactory storageFactory;
    private final ArchiveMetadataRepository archiveMetadataRepository;
    private final ArchiveMetadataMapper archiveMetadataMapper;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public BatchExecutionResult executeArchivingBatch(DataRetentionPolicy policy) {
        var startTime = LocalDateTime.now();
        
        log.info("아카이빙 배치 시작 - 정책: {}", policy.getPolicyId());
        
        try {
            // 아카이빙 대상 데이터 조회 - 순수 날짜 기반 처리
            var cutoffLocalDate = LocalDate.now().minusDays(policy.getRetentionRule().getDbRetentionDays());
            var cutoffDate = cutoffLocalDate.atTime(23, 59, 59); // 해당 날짜의 마지막 시간으로 설정 (inclusive)
            log.info("아카이빙 기준 날짜: {} (해당 날짜 이전 데이터 대상)", cutoffLocalDate);
            
            // 스토리지 초기화
            var storage = storageFactory.resolve();

            // 청크별로 데이터를 처리하되 일자별로 파일 1개만 생성
            var result = processDataInChunks(cutoffDate, policy, storage);
            
            var endTime = LocalDateTime.now();
            var executionTimeMs = java.time.Duration.between(startTime, endTime).toMillis();
            var throughputPerSecond = executionTimeMs > 0 ? (result.totalProcessed() * 1000.0 / executionTimeMs) : 0;
            
            log.info("아카이빙 배치 완료 - Processed: {}, Archived: {}, Deleted: {}, " +
                    "실행시간: {}ms, 처리량: {:.2f} records/sec", 
                    result.totalProcessed(), result.totalArchived(), result.totalDeleted(), 
                    executionTimeMs, throughputPerSecond);
            
            return BatchExecutionResult.success(
                result.totalProcessed(), 
                result.totalArchived(), 
                result.totalDeleted(),
                executionTimeMs,
                startTime, 
                endTime
            );
            
        } catch (Exception e) {
            var endTime = LocalDateTime.now();
            var failureResult = BatchExecutionResult.failure("배치 실행 실패: " + e.getMessage(), startTime, endTime);
            
            log.error("아카이빙 배치 실행 실패", e);
            return failureResult;
        }
    }

    /**
     * 메모리 효율적 청크별 데이터 처리 - 대량 데이터 최적화
     * 일자별로 단일 파일 생성하되, 메모리 사용량을 최소화
     */
    private ProcessResult processDataInChunks(LocalDateTime cutoffDate, DataRetentionPolicy policy, 
                                            Storage storage) {
        var dateToDataMap = new java.util.HashMap<LocalDate, java.util.List<AuthLog>>();
        var totalProcessed = 0L;
        var totalArchived = 0L;
        var totalDeleted = 0L;
        
        // 대량 데이터 처리를 위한 배치 크기 최적화 (1000 → 5000)
        var optimizedBatchSize = Math.max(policy.getRetentionRule().getBatchSize(), 5000);
        Long lastProcessedId = null;
        
        // 메모리 효율성을 위한 중간 처리 임계값
        final int MEMORY_OPTIMIZATION_THRESHOLD = 50; // 50개 날짜 그룹마다 중간 처리
        
        log.info("대량 데이터 배치 처리 시작 - 최적화된 배치 크기: {}, 메모리 최적화 임계값: {}", 
                optimizedBatchSize, MEMORY_OPTIMIZATION_THRESHOLD);
        
        // 청크별로 데이터 조회 및 메모리 효율적 처리
        while (true) {
            var chunk = authLogDbReader.findOlderThan(cutoffDate, optimizedBatchSize, lastProcessedId);
            
            if (chunk.isEmpty()) {
                break;
            }
            
            // 청크 데이터를 날짜별로 그룹화 (UTC → 애플리케이션 타임존 변환)
            for (var authLog : chunk) {
                var date = DateTimeUtils.toLocalDate(authLog.getDate());
                dateToDataMap.computeIfAbsent(date, k -> new ArrayList<>()).add(authLog);
            }
            
            totalProcessed += chunk.size();
            lastProcessedId = chunk.get(chunk.size() - 1).getId();
            
            log.info("청크 처리 완료 - 청크 크기: {}, 총 처리 건수: {}, 날짜별 그룹 수: {}", 
                    chunk.size(), totalProcessed, dateToDataMap.size());
            
            // 메모리 최적화: 일정 개수의 날짜 그룹이 모이면 중간 처리
            if (dateToDataMap.size() >= MEMORY_OPTIMIZATION_THRESHOLD) {
                log.info("메모리 최적화 중간 처리 실행 - 처리 대상 날짜 수: {}", dateToDataMap.size());
                var intermediateResult = createFilesByDateAndClear(dateToDataMap, policy, storage);
                totalArchived += intermediateResult.totalArchived();
                totalDeleted += intermediateResult.totalDeleted();
                
                log.info("중간 처리 완료 - Archived: {}, Deleted: {}, 남은 메모리 그룹: {}", 
                        intermediateResult.totalArchived(), intermediateResult.totalDeleted(), dateToDataMap.size());
            }
        }
        
        // 마지막 남은 데이터 처리
        if (!dateToDataMap.isEmpty()) {
            log.info("최종 데이터 처리 - 남은 날짜 그룹 수: {}", dateToDataMap.size());
            var finalResult = createFilesByDateAndClear(dateToDataMap, policy, storage);
            totalArchived += finalResult.totalArchived();
            totalDeleted += finalResult.totalDeleted();
        }
        
        if (totalProcessed == 0) {
            log.info("처리할 아카이빙 대상 데이터가 없습니다.");
            return new ProcessResult(0L, 0L, 0L);
        }
        
        log.info("대량 데이터 배치 처리 완료 - 총 처리: {}, 총 아카이브: {}, 총 삭제: {}", 
                totalProcessed, totalArchived, totalDeleted);
        
        return new ProcessResult(totalProcessed, totalArchived, totalDeleted);
    }
    
    /**
     * 메모리 효율적 날짜별 파일 생성 및 메모리 정리 (대량 데이터 최적화)
     */
    private ProcessResult createFilesByDateAndClear(Map<LocalDate, List<AuthLog>> dateToDataMap,
                                                  DataRetentionPolicy policy, Storage storage) {
        var totalProcessed = 0L;
        var totalArchived = 0L;
        var totalDeleted = 0L;
        
        // 처리할 날짜 목록을 미리 추출 (반복 중 맵 수정을 위해)
        var datesToProcess = new ArrayList<>(dateToDataMap.keySet());
        
        // 각 날짜별로 하나의 파일 생성 후 즉시 메모리에서 제거
        for (var date : datesToProcess) {
            var dateData = dateToDataMap.get(date);
            if (dateData == null || dateData.isEmpty()) {
                continue;
            }
            
            log.info("날짜 {} 데이터 처리 중 - 건수: {}", date, dateData.size());
            
            try {
                // 해당 날짜 데이터를 하나의 파일로 저장
                storage.store(dateData, date);
                
                // 아카이브 메타데이터 저장 (날짜별 단일 파일)
                saveArchiveMetadata(policy, dateData, storage, date);
                
                totalProcessed += dateData.size();
                totalArchived += dateData.size();
                
                // 데이터 삭제 (정책에 따라)
                if (policy.getRetentionRule().isEnableDataDeletion()) {
                    try {
                        // 실제 데이터 삭제 구현
                        var deletedCount = deleteDataByDate(dateData);
                        totalDeleted += deletedCount;
                        log.info("날짜 {} - 데이터 삭제 완료: {}건 (대상: {}건)", date, deletedCount, dateData.size());
                        
                        // 삭제 결과 검증
                        if (deletedCount != dateData.size()) {
                            log.warn("날짜 {} - 삭제 건수 불일치: 예상={}, 실제={}", 
                                    date, dateData.size(), deletedCount);
                        }
                    } catch (Exception e) {
                        log.error("날짜 {} - 데이터 삭제 실패: {}건", date, dateData.size(), e);
                        // 삭제 실패해도 아카이빙은 성공으로 처리
                    }
                }
                
                log.info("날짜 {} 처리 완료 - Processed: {}, Archived: {}", 
                        date, dateData.size(), dateData.size());
                
                // 메모리 최적화: 처리 완료된 날짜 데이터를 맵에서 즉시 제거
                dateToDataMap.remove(date);
                
            } catch (Exception e) {
                log.error("날짜 {} 데이터 처리 실패. 건수: {}", date, dateData.size(), e);
                // 실패한 날짜도 메모리에서 제거하여 무한 재시도 방지
                dateToDataMap.remove(date);
            }
        }
        
        return new ProcessResult(totalProcessed, totalArchived, totalDeleted);
    }

    /**
     * 날짜별로 파일 생성 (일자별 단일 파일) - 기존 방식
     */
    private ProcessResult createFilesByDate(Map<LocalDate, List<AuthLog>> dateToDataMap,
                                          DataRetentionPolicy policy, Storage storage) {
        var totalProcessed = 0L;
        var totalArchived = 0L;
        var totalDeleted = 0L;
        
        // 각 날짜별로 하나의 파일 생성
        for (var entry : dateToDataMap.entrySet()) {
            var date = entry.getKey();
            var dateData = entry.getValue();
            
            log.info("날짜 {} 데이터 처리 중 - 건수: {}", date, dateData.size());
            
            try {
                // 해당 날짜 데이터를 하나의 파일로 저장
                storage.store(dateData, date);
                
                // 아카이브 메타데이터 저장 (날짜별 단일 파일)
                saveArchiveMetadata(policy, dateData, storage, date);
                
                totalProcessed += dateData.size();
                totalArchived += dateData.size();
                
                // 데이터 삭제 (정책에 따라)
                if (policy.getRetentionRule().isEnableDataDeletion()) {
                    try {
                        // 실제 데이터 삭제 구현
                        var deletedCount = deleteDataByDate(dateData);
                        totalDeleted += deletedCount;
                        log.info("날짜 {} - 데이터 삭제 완룉: {}건 (대상: {}건)", date, deletedCount, dateData.size());
                        
                        // 삭제 결과 검증
                        if (deletedCount != dateData.size()) {
                            log.warn("날짜 {} - 삭제 건수 불일치: 예상={}, 실제={}", 
                                    date, dateData.size(), deletedCount);
                        }
                    } catch (Exception e) {
                        log.error("날짜 {} - 데이터 삭제 실패: {}건", date, dateData.size(), e);
                        // 삭제 실패해도 아카이빙은 성공으로 처리
                    }
                }
                
                log.info("날짜 {} 처리 완료 - Processed: {}, Archived: {}", 
                        date, dateData.size(), dateData.size());
                
            } catch (Exception e) {
                log.error("날짜 {} 데이터 처리 실패. 건수: {}", date, dateData.size(), e);
            }
        }
        
        return new ProcessResult(totalProcessed, totalArchived, totalDeleted);
    }
    
    /**
     * 처리 결과
     */
    private record ProcessResult(long totalProcessed, long totalArchived, long totalDeleted) {}
    
    /**
     * 아카이브 메타데이터를 데이터베이스에 저장 (날짜별 단일 파일) - 독립 트랜잭션
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    protected void saveArchiveMetadata(DataRetentionPolicy policy,
        List<AuthLog> targetData,
        Storage storage,
        LocalDate archiveDate) {
        try {
            // Master DB 강제 설정
            RoutingDataSourceContext.set("master");
            log.debug("Master DB 강제 설정 - 메타데이터 저장 작업");
            
            // 아카이브된 파일 경로 계산 (날짜별 단일 파일)
            var filePath = calculateArchiveFilePath(policy,archiveDate);
            
            // 파일 경로 기반 중복 방지: 이미 존재하는 파일인지 확인
            var existingMetadata = archiveMetadataRepository.findByFilePath(filePath);
            
            if (existingMetadata.isPresent()) {
                log.warn("이미 아카이브된 파일입니다. 파일: {}, 기존 메타데이터 ID: {}", 
                        filePath, existingMetadata.get().getId());
                return;
            }
            
            // 파일 크기 계산 (실제 파일이 존재하는 경우)
            var fileSizeBytes = calculateFileSize(filePath);
            
            // 날짜별 데이터의 날짜 범위 계산 (해당 날짜 전체 범위)
            var dateRange = calculateDateRangeForSingleDate(archiveDate);
            
            // ArchiveMetadata 생성 (날짜별 단일 메타데이터)
            var metadata = archiveMetadataMapper.createFromBatchResult(
                storage.getArchiveFormat().name().toLowerCase(),
                filePath,
                fileSizeBytes,
                dateRange.startDate(),
                dateRange.endDate(),
                policy.getArchivingStrategy().getCompressionType().getExtension(),
                targetData.size()
            );
            
            // 메타데이터 저장 (즉시 커밋됨)
            var savedMetadata = archiveMetadataRepository.save(metadata);
            
            log.info("아카이브 메타데이터 저장 완료. ID: {}, 파일: {}, 날짜: {}, 데이터건수: {}", 
                    savedMetadata.getId(), savedMetadata.getFilePath(), archiveDate, targetData.size());
                    
        } catch (Exception e) {
            log.error("아카이브 메타데이터 저장 실패. 날짜: {}, 데이터건수: {}", archiveDate, targetData.size(), e);
            // 메타데이터 저장 실패는 전체 배치를 실패시키지 않음 (비즈니스 판단)
        } finally {
            // DataSource 컨텍스트 정리
            RoutingDataSourceContext.clear();
        }
    }
    
    /**
     * 아카이브 파일 경로 계산 (날짜별 단일 파일)
     */
    private String calculateArchiveFilePath(DataRetentionPolicy policy, LocalDate archiveDate) {
        var archiveBasePath = policy.getArchivingStrategy().getArchiveBasePath();
        
        // 연도/월/일 기반 디렉토리 구조
        var year = String.valueOf(archiveDate.getYear());
        var month = String.format("%02d", archiveDate.getMonthValue());
        
        // 파일명 생성: 날짜.확장자.압축확장자
        var fileName = String.format("%s%s%s",
                archiveDate.format(DATE_FORMATTER),
                policy.getArchivingStrategy().getArchiveFormat().getExtension(),
                policy.getArchivingStrategy().getCompressionType().getExtension()
        );
        
        // 체계적인 디렉토리 구조: /archive/2025/01/2025-01-15.sql.gz
        return Paths.get(archiveBasePath, year, month, fileName).toString();
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
     * 날짜별 데이터의 날짜 범위 계산 (해당 날짜 전체 범위)
     * 예: 2024-01-15 데이터라면 2024-01-15T00:00:00Z ~ 2024-01-15T23:59:59Z
     */
    private DateRange calculateDateRangeForSingleDate(LocalDate date) {
        var startOfDay = DateTimeUtils.atStartOfDay(date);
        var endOfDay = DateTimeUtils.atEndOfDay(date);
        
        return new DateRange(startOfDay, endOfDay);
    }
    
    
    /**
     * 날짜 범위를 나타내는 레코드
     */
    private record DateRange(Instant startDate, Instant endDate) {}
    
    /**
     * 실제 데이터 삭제 수행 (Master DB에서 삭제) - 독립 트랜잭션
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    protected long deleteDataByDate(List<AuthLog> dateData) {
        if (dateData.isEmpty()) {
            return 0L;
        }
        
        try {
            // Master DB 강제 설정
            RoutingDataSourceContext.set("master");
            log.info("Master DB 강제 설정 - 데이터 삭제 작업");
            // ID 목록 추출
            var idsToDelete = dateData.stream()
                .map(AuthLog::getId)
                .toList();
            
            log.info("데이터 삭제 시작 - 대상 ID 개수: {}", idsToDelete.size());
            
            // 배치 삭제 실행 (즉시 커밋됨)
            var deletedCount = authLogDbReader.deleteByIds(idsToDelete);
            
            log.info("데이터 삭제 완료 (Master DB) - 요청: {}건, 실제 삭제: {}건", idsToDelete.size(), deletedCount);
            
            return deletedCount;
            
        } catch (Exception e) {
            log.error("데이터 삭제 중 오류 발생. 대상 건수: {}", dateData.size(), e);
            throw new RuntimeException("데이터 삭제 실패", e);
        } finally {
            // DataSource 컨텍스트 정리
            RoutingDataSourceContext.clear();
        }
    }

}