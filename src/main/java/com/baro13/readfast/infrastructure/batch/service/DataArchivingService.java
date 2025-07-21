package com.baro13.readfast.infrastructure.batch.service;

import com.baro13.readfast.domain.AuthLog;
import com.baro13.readfast.infrastructure.db.jpa.AuthLogEntity;
import com.baro13.readfast.infrastructure.db.jpa.AuthLogMapper;
import com.baro13.readfast.infrastructure.db.jpa.AuthQueryJpaRepository;
import com.baro13.readfast.infrastructure.policy.DataRetentionProperties;
import com.baro13.readfast.infrastructure.storage.StorageService;
import com.baro13.readfast.infrastructure.storage.service.AnalyticsService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 데이터 아카이빙 비즈니스 로직을 담당하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataArchivingService {
    
    private final AuthQueryJpaRepository authQueryJpaRepository;
    private final StorageService storageService;
    private final AnalyticsService analyticsService;
    private final DataRetentionProperties properties;
    private final @Qualifier("taskExecutor") Executor taskExecutor;
    
    /**
     * 아카이빙 대상 데이터 조회 (페이징)
     */
    public Page<AuthLogEntity> getArchiveTargetData(Pageable pageable) {
        Instant cutoffDate = Instant.now().minus(properties.getDbRetentionDays(), ChronoUnit.DAYS);
        return authQueryJpaRepository.findByDateBefore(cutoffDate, pageable);
    }
    
    /**
     * 데이터를 스토리지에 아카이빙
     */
    @Transactional
    public void archiveData(List<AuthLogEntity> entities) {
        if (entities.isEmpty()) {
            return;
        }
        
        // 엔티티를 도메인 객체로 변환
        List<AuthLog> authLogs = entities.stream()
                .map(AuthLogMapper::toDomain)
                .toList();
        
        // 날짜별로 그룹화
        Map<LocalDate, List<AuthLog>> groupedByDate = authLogs.stream()
                .collect(Collectors.groupingBy(
                        authLog -> authLog.getDate().atZone(ZoneId.systemDefault()).toLocalDate()
                ));
        
        // 각 날짜별로 저장
        groupedByDate.forEach((date, logs) -> {
            try {
                storageService.storeData(logs, date);
                log.info("{}일자 {}개 레코드 아카이빙 완료", date, logs.size());
            } catch (Exception e) {
                log.error("{}일자 데이터 아카이빙 실패", date, e);
                throw new RuntimeException("데이터 아카이빙 실패", e);
            }
        });
    }
    
    /**
     * SQLite 변환 작업 실행
     */
    public void convertToSQLite() {
        if (!properties.isEnableSqliteConversion()) {
            log.info("SQLite 변환이 비활성화되어 있습니다.");
            return;
        }
        
        LocalDate cutoffDate = LocalDate.now().minusDays(properties.getDbRetentionDays());
        LocalDate endDate = LocalDate.now().minusDays(1);
        
        // 병렬로 압축 파일을 SQLite로 변환
        List<CompletableFuture<Void>> conversionFutures = cutoffDate.datesUntil(endDate.plusDays(1))
                .map(date -> CompletableFuture.runAsync(() -> {
                    try {
                        long startTime = System.currentTimeMillis();
                        java.nio.file.Path compressedFilePath = getCompressedFilePath(date);
                        if (java.nio.file.Files.exists(compressedFilePath)) {
                            analyticsService.convertCompressedFile(compressedFilePath, date);
                            long duration = System.currentTimeMillis() - startTime;
                            log.info("{}일자 SQLite 변환 완료 ({}ms)", date, duration);
                        }
                    } catch (Exception e) {
                        log.error("{}일자 SQLite 변환 실패", date, e);
                    }
                }, taskExecutor))
                .toList();
        
        // 모든 변환 작업 완료 대기
        CompletableFuture.allOf(conversionFutures.toArray(new CompletableFuture[0]))
                .join();
        
        // 분석용 통합 SQLite DB 생성
        analyticsService.createAnalyticsDatabase(cutoffDate, endDate);
        
        log.info("SQLite 변환 및 통합 DB 생성 완료: {} ~ {}", cutoffDate, endDate);
    }
    
    /**
     * 아카이빙된 데이터 삭제
     */
    @Transactional
    public void cleanupArchivedData(List<AuthLogEntity> entities) {
        if (!properties.isEnableDataDeletion()) {
            log.info("데이터 삭제가 비활성화되어 있습니다.");
            return;
        }
        
        if (entities.isEmpty()) {
            return;
        }
        
        authQueryJpaRepository.deleteAll(entities);
        log.info("데이터베이스에서 이전 레코드 {} 개 삭제 완료", entities.size());
    }
    
    /**
     * 아카이빙 대상 전체 개수 조회
     */
    public long getArchiveTargetCount() {
        Instant cutoffDate = Instant.now().minus(properties.getDbRetentionDays(), ChronoUnit.DAYS);
        return authQueryJpaRepository.countByDateBefore(cutoffDate);
    }
    
    /**
     * 기본 페이지 요청 객체 생성
     */
    public Pageable createDefaultPageable() {
        return PageRequest.of(0, properties.getBatchSize(), Sort.by("date").ascending());
    }
    
    private java.nio.file.Path getCompressedFilePath(LocalDate date) {
        String dateStr = date.format(java.time.format.DateTimeFormatter.ofPattern(properties.getArchiveFileFormat()));
        String extension = getCompressedFileExtension();
        String fileName = dateStr + extension;
        return java.nio.file.Paths.get(properties.getArchiveBasePath(), fileName);
    }
    
    private String getCompressedFileExtension() {
        String dataFormat = properties.getArchiveDataFormat();
        boolean isCompressed = properties.isEnableCompression() && "gzip".equals(properties.getCompressionFormat());
        
        if ("csv".equals(dataFormat)) {
            return isCompressed ? ".csv.gz" : ".csv";
        } else if ("json".equals(dataFormat)) {
            return isCompressed ? ".json.gz" : ".json";
        }
        return isCompressed ? ".json.gz" : ".json";
    }
}