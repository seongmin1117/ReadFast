package com.baro13.readfast.admin.authlog.adapter.in.controller;

import com.baro13.readfast.admin.authlog.adapter.out.archive.cache.ArchiveCache;
import com.baro13.readfast.admin.authlog.adapter.out.archive.storage.SqliteStorage;
import com.baro13.readfast.global.response.ApiResponse;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
public class CacheMonitoringController {

    private final ArchiveCache archiveCache;
    
    @Autowired(required = false)
    private SqliteStorage sqliteStorage;

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheStatistics() {
        try {
            ArchiveCache.CacheStatistics stats = archiveCache.getStatistics();
            
            Map<String, Object> result = new HashMap<>();
            result.put("decompressedCache", Map.of(
                "size", stats.getDecompressedCacheSize(),
                "hitRate", String.format("%.2f%%", stats.getDecompressedHitRate() * 100),
                "evictionCount", stats.getDecompressedEvictionCount()
            ));
            
            result.put("compressedCache", Map.of(
                "size", stats.getCompressedCacheSize(),
                "hitRate", String.format("%.2f%%", stats.getCompressedHitRate() * 100),
                "evictionCount", stats.getCompressedEvictionCount()
            ));
            
            result.put("summary", Map.of(
                "totalCacheSize", stats.getDecompressedCacheSize() + stats.getCompressedCacheSize(),
                "overallHitRate", String.format("%.2f%%", 
                    ((stats.getDecompressedHitRate() + stats.getCompressedHitRate()) / 2) * 100),
                "totalEvictions", stats.getDecompressedEvictionCount() + stats.getCompressedEvictionCount()
            ));
            
            log.info("캐시 통계 조회 완료");
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("캐시 통계 조회 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("캐시 통계 조회 중 오류가 발생했습니다"));
        }
    }

    @DeleteMapping("/evict/{date}")
    public ResponseEntity<ApiResponse<String>> evictCache(@PathVariable String date) {
        try {
            LocalDate targetDate = LocalDate.parse(date);
            archiveCache.evict(targetDate);
            
            String message = String.format("날짜 %s의 캐시가 성공적으로 무효화되었습니다", date);
            log.info("캐시 무효화 완료: {}", date);
            return ResponseEntity.ok(ApiResponse.success(message));
            
        } catch (Exception e) {
            log.error("캐시 무효화 실패: {}", date, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("캐시 무효화 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @DeleteMapping("/evict-all")
    public ResponseEntity<ApiResponse<String>> evictAllCache() {
        try {
            archiveCache.evictAll();
            
            String message = "모든 아카이브 캐시가 성공적으로 무효화되었습니다";
            log.info("전체 캐시 무효화 완료");
            return ResponseEntity.ok(ApiResponse.success(message));
            
        } catch (Exception e) {
            log.error("전체 캐시 무효화 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("전체 캐시 무효화 중 오류가 발생했습니다"));
        }
    }

    @PostMapping("/cleanup")
    public ResponseEntity<ApiResponse<String>> cleanupCache() {
        try {
            archiveCache.cleanup();
            
            String message = "캐시 정리가 성공적으로 완료되었습니다";
            log.info("캐시 정리 완료");
            return ResponseEntity.ok(ApiResponse.success(message));
            
        } catch (Exception e) {
            log.error("캐시 정리 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("캐시 정리 중 오류가 발생했습니다"));
        }
    }

    @PostMapping("/cleanup-temp-files")
    public ResponseEntity<ApiResponse<String>> cleanupTemporaryFiles() {
        try {
            if (sqliteStorage != null) {
                sqliteStorage.cleanupOldTemporaryFiles();
                String message = "임시 파일 정리가 성공적으로 완료되었습니다";
                log.info("임시 파일 정리 완료");
                return ResponseEntity.ok(ApiResponse.success(message));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("SQLite 스토리지가 활성화되지 않았습니다"));
            }
            
        } catch (Exception e) {
            log.error("임시 파일 정리 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("임시 파일 정리 중 오류가 발생했습니다"));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheHealth() {
        try {
            ArchiveCache.CacheStatistics stats = archiveCache.getStatistics();
            
            // 간단한 헬스 체크 로직
            boolean isHealthy = true;
            String status = "HEALTHY";
            
            // 캐시 히트율이 너무 낮으면 건강하지 않다고 판단
            double averageHitRate = (stats.getDecompressedHitRate() + stats.getCompressedHitRate()) / 2;
            if (averageHitRate < 0.3) { // 30% 미만
                isHealthy = false;
                status = "LOW_HIT_RATE";
            }
            
            // 캐시 크기가 0이면 문제가 있을 수 있음
            if (stats.getDecompressedCacheSize() == 0 && stats.getCompressedCacheSize() == 0) {
                status = "EMPTY";
            }
            
            Map<String, Object> health = Map.of(
                "status", status,
                "healthy", isHealthy,
                "averageHitRate", String.format("%.2f%%", averageHitRate * 100),
                "totalCacheSize", stats.getDecompressedCacheSize() + stats.getCompressedCacheSize(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(ApiResponse.success(health));
            
        } catch (Exception e) {
            log.error("캐시 헬스 체크 실패", e);
            Map<String, Object> health = Map.of(
                "status", "ERROR",
                "healthy", false,
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("캐시 헬스 체크 중 오류가 발생했습니다"));
        }
    }
}