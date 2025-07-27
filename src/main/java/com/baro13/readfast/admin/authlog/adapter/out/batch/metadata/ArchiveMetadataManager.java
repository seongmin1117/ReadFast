package com.baro13.readfast.admin.authlog.adapter.out.batch.metadata;

import com.baro13.readfast.admin.authlog.adapter.out.archive.storage.Storage;
import com.baro13.readfast.admin.authlog.adapter.out.db.jpa.mapper.ArchiveMetadataMapper;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.admin.authlog.domain.port.ArchiveMetadataRepository;
import com.baro13.readfast.admin.policy.domain.model.DataRetentionPolicy;
import com.baro13.readfast.global.common.TimeZoneConstants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 아카이브 메타데이터 관리자
 * 아카이브된 파일의 메타데이터 생성, 저장, 관리를 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveMetadataManager {
    
    private final ArchiveMetadataRepository archiveMetadataRepository;
    private final ArchiveMetadataMapper archiveMetadataMapper;
    private final ArchivePathCalculator pathCalculator;
    
    /**
     * 아카이브 메타데이터 저장
     */
    public void saveArchiveMetadata(DataRetentionPolicy policy, 
                                  List<AuthLog> targetData,
                                  Storage storage,
                                  int chunkNumber) {
        try {
            // 파일 경로 계산
            var filePath = pathCalculator.calculateArchiveFilePath(policy, targetData, storage, chunkNumber);
            
            // 중복 체크
            if (isMetadataAlreadyExists(filePath)) {
                log.warn("이미 존재하는 아카이브 메타데이터. 파일: {}", filePath);
                return;
            }
            
            // 메타데이터 생성 및 저장
            var metadata = createArchiveMetadata(storage, filePath, targetData);
            var savedMetadata = archiveMetadataRepository.save(metadata);
            
            log.info("아카이브 메타데이터 저장 완료. ID: {}, 파일: {}, 청크: #{}, 데이터건수: {}", 
                    savedMetadata.getId(), savedMetadata.getFilePath(), chunkNumber, targetData.size());
                    
        } catch (Exception e) {
            log.error("아카이브 메타데이터 저장 실패. 청크: #{}, 데이터건수: {}", chunkNumber, targetData.size(), e);
            // 메타데이터 저장 실패는 전체 배치를 실패시키지 않음
        }
    }
    
    private boolean isMetadataAlreadyExists(String filePath) {
        var existingMetadata = archiveMetadataRepository.findByFilePath(filePath);
        
        if (existingMetadata.isPresent()) {
            log.warn("이미 아카이브된 파일입니다. 파일: {}, 기존 메타데이터 ID: {}", 
                    filePath, existingMetadata.get().getId());
            return true;
        }
        
        return false;
    }
    
    private com.baro13.readfast.admin.authlog.domain.model.ArchiveMetadata createArchiveMetadata(
            Storage storage, String filePath, List<AuthLog> targetData) {
        
        // 파일 크기 계산
        var fileSizeBytes = calculateFileSize(filePath);
        
        // 데이터 날짜 범위 계산
        var dateRange = calculateDateRange(targetData);
        
        return archiveMetadataMapper.createFromBatchResult(
            storage.getArchiveFormat().name().toLowerCase(),
            filePath,
            fileSizeBytes,
            dateRange.startDate(),
            dateRange.endDate()
        );
    }
    
    private Long calculateFileSize(String filePath) {
        try {
            var path = Paths.get(filePath);
            if (Files.exists(path)) {
                return Files.size(path);
            }
        } catch (IOException e) {
            log.warn("파일 크기 계산 실패. 파일: {}", filePath, e);
        }
        return null;
    }
    
    private DateRange calculateDateRange(List<AuthLog> targetData) {
        if (targetData.isEmpty()) {
            var now = Instant.now();
            return new DateRange(now, now);
        }
        
        var minDate = targetData.stream()
            .map(AuthLog::getDate)
            .min(Instant::compareTo)
            .orElse(Instant.now());
            
        var maxDate = targetData.stream()
            .map(AuthLog::getDate)
            .max(Instant::compareTo)
            .orElse(Instant.now());
            
        return new DateRange(minDate, maxDate);
    }
    
    /**
     * 날짜 범위 레코드
     */
    private record DateRange(Instant startDate, Instant endDate) {}
}