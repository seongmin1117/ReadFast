package com.baro13.readfast.admin.authlog.adapter.out.batch.metadata;

import com.baro13.readfast.admin.authlog.adapter.out.archive.storage.Storage;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.admin.policy.domain.model.DataRetentionPolicy;
import com.baro13.readfast.global.common.TimeZoneConstants;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 아카이브 파일 경로 계산기
 * 체계적이고 중복 방지된 아카이브 파일 경로를 생성
 */
@Slf4j
@Component
public class ArchivePathCalculator {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * 아카이브 파일 경로 계산
     */
    public String calculateArchiveFilePath(DataRetentionPolicy policy, 
                                         List<AuthLog> targetData, 
                                         Storage storage, 
                                         int chunkNumber) {
        
        var archiveBasePath = policy.getArchivingStrategy().getArchiveBasePath();
        var batchStartTime = LocalDateTime.now(); // 실제로는 배치 시작 시간을 전달받아야 함
        
        // 데이터의 날짜 범위 계산
        var dateRange = calculateDateRange(targetData);
        var startDate = dateRange.startDate().atZone(TimeZoneConstants.APPLICATION_ZONE).toLocalDate();
        var endDate = dateRange.endDate().atZone(TimeZoneConstants.APPLICATION_ZONE).toLocalDate();
        
        // 디렉토리 구조 생성
        var directoryPath = createDirectoryStructure(archiveBasePath, startDate);
        
        // 파일명 생성
        var fileName = createFileName(startDate, endDate, chunkNumber, targetData.size(), 
                                    batchStartTime, storage);
        
        var fullPath = Paths.get(directoryPath, fileName).toString();
        
        log.debug("아카이브 파일 경로 생성 - 경로: {}, 청크: #{}, 레코드수: {}", 
                 fullPath, chunkNumber, targetData.size());
        
        return fullPath;
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
    
    private String createDirectoryStructure(String basePath, java.time.LocalDate date) {
        var year = String.valueOf(date.getYear());
        var month = String.format("%02d", date.getMonthValue());
        var day = String.format("%02d", date.getDayOfMonth());
        
        return Paths.get(basePath, year, month, day).toString();
    }
    
    private String createFileName(java.time.LocalDate startDate, 
                                java.time.LocalDate endDate,
                                int chunkNumber, 
                                int recordCount, 
                                LocalDateTime batchStartTime,
                                Storage storage) {
        
        var batchTimestamp = batchStartTime.format(DateTimeFormatter.ofPattern("HHmmss"));
        var extension = storage.getArchiveFormat().getExtension();
        
        if (startDate.equals(endDate)) {
            // 단일 날짜: 2024-01-15_chunk001_1000_143052.json
            return String.format("%s_chunk%03d_%d_%s%s", 
                    startDate.format(DATE_FORMATTER), 
                    chunkNumber, 
                    recordCount, 
                    batchTimestamp,
                    extension);
        } else {
            // 다중 날짜: 2024-01-15_to_2024-01-17_chunk001_1000_143052.json
            return String.format("%s_to_%s_chunk%03d_%d_%s%s", 
                    startDate.format(DATE_FORMATTER), 
                    endDate.format(DATE_FORMATTER), 
                    chunkNumber, 
                    recordCount, 
                    batchTimestamp,
                    extension);
        }
    }
    
    /**
     * 날짜 범위 레코드
     */
    private record DateRange(Instant startDate, Instant endDate) {}
}