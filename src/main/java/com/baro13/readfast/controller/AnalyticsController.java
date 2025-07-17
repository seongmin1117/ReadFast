package com.baro13.readfast.controller;

import com.baro13.readfast.infrastructure.storage.AnalyticsStorageFactory;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    
    private final AnalyticsStorageFactory analyticsStorageFactory;
    
    /**
     * 분석용 통합 SQLite 갱신
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshAnalyticsDatabase(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            // 압축 파일을 SQLite로 변환 및 통합 DB 생성
            startDate.datesUntil(endDate.plusDays(1))
                    .forEach(date -> {
                        try {
                            java.nio.file.Path compressedFilePath = getCompressedFilePath(date);
                            if (java.nio.file.Files.exists(compressedFilePath)) {
                                analyticsStorageFactory.convertCompressedFile(compressedFilePath, date);
                            }
                        } catch (Exception e) {
                            log.error("{}일자 SQLite 변환 실패", date, e);
                        }
                    });
            
            // 분석용 통합 SQLite 생성
            analyticsStorageFactory.createAnalyticsDatabase(startDate, endDate);
            
            log.info("분석용 SQLite 갱신 완료: {} ~ {}", startDate, endDate);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "분석용 SQLite 데이터베이스 갱신 완료",
                "startDate", startDate,
                "endDate", endDate
            ));
            
        } catch (Exception e) {
            log.error("분석용 SQLite 갱신 실패: {} ~ {}", startDate, endDate, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "분석용 SQLite 데이터베이스 갱신 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 특정 날짜의 SQLite 파일 변환
     */
    @PostMapping("/convert")
    public ResponseEntity<Map<String, Object>> convertToSQLite(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        try {
            java.nio.file.Path compressedFilePath = getCompressedFilePath(date);
            if (java.nio.file.Files.exists(compressedFilePath)) {
                analyticsStorageFactory.convertCompressedFile(compressedFilePath, date);
            }
            
            log.info("SQLite 변환 완료: {}", date);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "SQLite 변환 완료",
                "date", date
            ));
            
        } catch (Exception e) {
            log.error("SQLite 변환 실패: {}", date, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "SQLite 변환 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 압축 파일 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getFileStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        boolean compressedExists = isCompressedFileExists(date);
        boolean sqliteExists = isSQLiteFileExists(date);
        
        return ResponseEntity.ok(Map.of(
            "date", date,
            "compressedFileExists", compressedExists,
            "sqliteFileExists", sqliteExists,
            "status", sqliteExists ? "ready" : (compressedExists ? "need_conversion" : "not_available")
        ));
    }
    
    /**
     * 오래된 SQLite 파일 정리
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupOldFiles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cutoffDate) {
        
        try {
            cleanupOldSQLiteFiles(cutoffDate);
            
            log.info("오래된 SQLite 파일 정리 완료: {} 이전", cutoffDate);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "오래된 파일 정리 완료",
                "cutoffDate", cutoffDate
            ));
            
        } catch (Exception e) {
            log.error("파일 정리 실패: {}", cutoffDate, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "파일 정리 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 날짜 범위의 파일 상태 조회
     */
    @GetMapping("/status/range")
    public ResponseEntity<Map<String, Object>> getFileStatusRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<LocalDate, Map<String, Object>> statusMap = startDate.datesUntil(endDate.plusDays(1))
                .collect(java.util.stream.Collectors.toMap(
                    date -> date,
                    date -> Map.of(
                        "compressedFileExists", isCompressedFileExists(date),
                        "sqliteFileExists", isSQLiteFileExists(date)
                    )
                ));
        
        return ResponseEntity.ok(Map.of(
            "startDate", startDate,
            "endDate", endDate,
            "fileStatus", statusMap
        ));
    }
    
    // 헬퍼 메서드들
    private java.nio.file.Path getCompressedFilePath(LocalDate date) {
        String dateStr = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String fileName = dateStr + ".csv.gz";
        return java.nio.file.Paths.get("./archive-data", fileName);
    }
    
    private boolean isCompressedFileExists(LocalDate date) {
        return java.nio.file.Files.exists(getCompressedFilePath(date));
    }
    
    private boolean isSQLiteFileExists(LocalDate date) {
        String dateStr = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String fileName = dateStr + ".db";
        java.nio.file.Path sqlitePath = java.nio.file.Paths.get("./archive-data", "sqlite", fileName);
        return java.nio.file.Files.exists(sqlitePath);
    }
    
    private void cleanupOldSQLiteFiles(LocalDate cutoffDate) {
        java.nio.file.Path sqliteDirectory = java.nio.file.Paths.get("./archive-data", "sqlite");
        
        if (!java.nio.file.Files.exists(sqliteDirectory)) {
            return;
        }
        
        try {
            java.nio.file.Files.list(sqliteDirectory)
                .filter(java.nio.file.Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".db"))
                .forEach(path -> {
                    try {
                        String fileName = path.getFileName().toString();
                        String dateStr = fileName.substring(0, fileName.lastIndexOf('.'));
                        LocalDate fileDate = LocalDate.parse(dateStr);
                        
                        if (fileDate.isBefore(cutoffDate)) {
                            java.nio.file.Files.delete(path);
                            log.info("오래된 SQLite 파일 정리: {}", path);
                        }
                    } catch (Exception e) {
                        log.error("SQLite 파일 정리 실패: {}", path, e);
                    }
                });
        } catch (Exception e) {
            log.error("SQLite 디렉토리 정리 실패", e);
        }
    }
}