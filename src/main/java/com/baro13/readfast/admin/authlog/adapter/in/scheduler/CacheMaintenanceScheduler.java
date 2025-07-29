package com.baro13.readfast.admin.authlog.adapter.in.scheduler;

import com.baro13.readfast.admin.authlog.adapter.out.archive.cache.ArchiveCache;
import com.baro13.readfast.admin.authlog.adapter.out.archive.storage.OptimizedSqliteStorage;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 캐시 및 임시 파일 유지 관리 스케줄러
 * - 정기적인 캐시 정리
 * - 임시 파일 자동 정리
 * - 캐시 통계 로깅
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class CacheMaintenanceScheduler {

    private final ArchiveCache archiveCache;
    private final Optional<OptimizedSqliteStorage> sqliteStorage;

    /**
     * 캐시 정리 작업 - 매 30분마다 실행
     * 만료된 캐시 항목을 정리하고 메모리 효율성을 개선
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30분
    public void cleanupCache() {
        try {
            log.debug("캐시 정리 작업 시작");
            
            // 캐시 정리 실행
            archiveCache.cleanup();
            
            // 캐시 통계 로깅 (디버그 레벨)
            ArchiveCache.CacheStatistics stats = archiveCache.getStatistics();
            log.debug("캐시 정리 완료 - 압축해제캐시: {}개, 압축캐시: {}개, 전체히트율: {:.2f}%",
                     stats.getDecompressedCacheSize(),
                     stats.getCompressedCacheSize(),
                     ((stats.getDecompressedHitRate() + stats.getCompressedHitRate()) / 2) * 100);
                     
        } catch (Exception e) {
            log.error("캐시 정리 작업 실패", e);
        }
    }

    /**
     * 임시 파일 정리 작업 - 매 2시간마다 실행
     * 압축 해제 시 생성된 임시 파일들을 정리
     */
    @Scheduled(fixedRate = 2 * 60 * 60 * 1000) // 2시간
    public void cleanupTemporaryFiles() {
        if (sqliteStorage.isEmpty()) {
            log.debug("SQLite 스토리지가 활성화되지 않아 임시 파일 정리를 건너뜁니다");
            return;
        }
        
        try {
            log.debug("임시 파일 정리 작업 시작");
            
            sqliteStorage.get().cleanupOldTemporaryFiles();
            
            log.debug("임시 파일 정리 작업 완료");
            
        } catch (Exception e) {
            log.error("임시 파일 정리 작업 실패", e);
        }
    }

    /**
     * 캐시 성능 통계 로깅 - 매 1시간마다 실행
     * 캐시 성능 모니터링을 위한 상세 통계 로깅
     */
    @Scheduled(fixedRate = 60 * 60 * 1000) // 1시간
    public void logCacheStatistics() {
        try {
            ArchiveCache.CacheStatistics stats = archiveCache.getStatistics();
            
            log.info("=== 아카이브 캐시 성능 통계 ===");
            log.info("압축 해제 캐시 - 크기: {}, 히트율: {:.2f}%, 제거횟수: {}",
                     stats.getDecompressedCacheSize(),
                     stats.getDecompressedHitRate() * 100,
                     stats.getDecompressedEvictionCount());
            log.info("압축 데이터 캐시 - 크기: {}, 히트율: {:.2f}%, 제거횟수: {}",
                     stats.getCompressedCacheSize(),
                     stats.getCompressedHitRate() * 100,
                     stats.getCompressedEvictionCount());
            log.info("전체 캐시 크기: {}, 평균 히트율: {:.2f}%",
                     stats.getDecompressedCacheSize() + stats.getCompressedCacheSize(),
                     ((stats.getDecompressedHitRate() + stats.getCompressedHitRate()) / 2) * 100);
            log.info("================================");
            
        } catch (Exception e) {
            log.error("캐시 통계 로깅 실패", e);
        }
    }

    /**
     * 캐시 건강 상태 체크 - 매 6시간마다 실행
     * 캐시 성능 이슈를 조기에 감지하고 알림
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // 6시간
    public void checkCacheHealth() {
        try {
            ArchiveCache.CacheStatistics stats = archiveCache.getStatistics();
            
            double averageHitRate = (stats.getDecompressedHitRate() + stats.getCompressedHitRate()) / 2;
            long totalCacheSize = stats.getDecompressedCacheSize() + stats.getCompressedCacheSize();
            
            // 성능 이슈 감지 및 경고
            if (averageHitRate < 0.3) { // 히트율 30% 미만
                log.warn("⚠️ 캐시 히트율이 낮습니다: {:.2f}% - 캐시 전략 검토가 필요합니다", 
                        averageHitRate * 100);
            }
            
            if (totalCacheSize == 0) {
                log.warn("⚠️ 캐시가 비어있습니다 - 캐시 로딩에 문제가 있을 수 있습니다");
            }
            
            if (stats.getDecompressedEvictionCount() > 1000 || stats.getCompressedEvictionCount() > 1000) {
                log.warn("⚠️ 캐시 제거 횟수가 많습니다 (압축해제: {}, 압축: {}) - 캐시 크기 증설을 고려하세요",
                        stats.getDecompressedEvictionCount(), stats.getCompressedEvictionCount());
            }
            
            // 정상 상태일 때는 INFO 레벨로 간단히 로깅
            if (averageHitRate >= 0.5 && totalCacheSize > 0) {
                log.info("✅ 캐시 상태 양호 - 히트율: {:.2f}%, 크기: {}개", 
                        averageHitRate * 100, totalCacheSize);
            }
            
        } catch (Exception e) {
            log.error("캐시 건강 상태 체크 실패", e);
        }
    }
}