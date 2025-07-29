package com.baro13.readfast.admin.authlog.adapter.out.archive.cache;

import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveCache {

    @Qualifier("archiveCaffeineCache")
    private final Cache<LocalDate, List<AuthLog>> decompressedCache;
    
    @Qualifier("compressedDataCache")
    private final Cache<LocalDate, byte[]> compressedCache;

    /**
     * 압축 해제된 AuthLog 데이터 조회
     */
    public Optional<List<AuthLog>> getDecompressed(LocalDate date) {
        return Optional.ofNullable(decompressedCache.getIfPresent(date));
    }

    /**
     * 압축 해제된 AuthLog 데이터 저장
     */
    public void putDecompressed(LocalDate date, List<AuthLog> authLogs) {
        if (authLogs != null && !authLogs.isEmpty()) {
            decompressedCache.put(date, authLogs);
            log.debug("압축 해제된 데이터 캐시 저장: 날짜={}, 레코드={}", date, authLogs.size());
        }
    }

    /**
     * 압축된 바이트 데이터 조회
     */
    public Optional<byte[]> getCompressed(LocalDate date) {
        return Optional.ofNullable(compressedCache.getIfPresent(date));
    }

    /**
     * 압축된 바이트 데이터 저장
     */
    public void putCompressed(LocalDate date, byte[] compressedData) {
        if (compressedData != null && compressedData.length > 0) {
            compressedCache.put(date, compressedData);
            log.debug("압축된 데이터 캐시 저장: 날짜={}, 크기={}bytes", date, compressedData.length);
        }
    }

    /**
     * 기존 API 호환성을 위한 메서드 (Deprecated)
     */
    @Deprecated
    public List<AuthLog> get(LocalDate date) {
        return getDecompressed(date).orElse(null);
    }

    /**
     * 기존 API 호환성을 위한 메서드 (Deprecated)
     */
    @Deprecated
    public void put(LocalDate date, List<AuthLog> authLogs) {
        putDecompressed(date, authLogs);
    }

    /**
     * 특정 날짜의 캐시 무효화
     */
    public void evict(LocalDate date) {
        decompressedCache.invalidate(date);
        compressedCache.invalidate(date);
        log.debug("캐시 무효화: 날짜={}", date);
    }

    /**
     * 전체 캐시 무효화
     */
    public void evictAll() {
        decompressedCache.invalidateAll();
        compressedCache.invalidateAll();
        log.info("전체 아카이브 캐시 무효화");
    }

    /**
     * 캐시 통계 정보 조회
     */
    public CacheStatistics getStatistics() {
        CacheStats decompressedStats = decompressedCache.stats();
        CacheStats compressedStats = compressedCache.stats();
        
        return CacheStatistics.builder()
            .decompressedCacheSize(decompressedCache.estimatedSize())
            .compressedCacheSize(compressedCache.estimatedSize())
            .decompressedHitRate(decompressedStats.hitRate())
            .compressedHitRate(compressedStats.hitRate())
            .decompressedEvictionCount(decompressedStats.evictionCount())
            .compressedEvictionCount(compressedStats.evictionCount())
            .build();
    }

    /**
     * 캐시 정리 (수동 정리 메서드)
     */
    public void cleanup() {
        decompressedCache.cleanUp();
        compressedCache.cleanUp();
        log.debug("아카이브 캐시 정리 완료");
    }

    /**
     * 캐시 통계 정보를 담는 클래스
     */
    @Getter
    public static class CacheStatistics {

        private final long decompressedCacheSize;
        private final long compressedCacheSize;
        private final double decompressedHitRate;
        private final double compressedHitRate;
        private final long decompressedEvictionCount;
        private final long compressedEvictionCount;

        private CacheStatistics(Builder builder) {
            this.decompressedCacheSize = builder.decompressedCacheSize;
            this.compressedCacheSize = builder.compressedCacheSize;
            this.decompressedHitRate = builder.decompressedHitRate;
            this.compressedHitRate = builder.compressedHitRate;
            this.decompressedEvictionCount = builder.decompressedEvictionCount;
            this.compressedEvictionCount = builder.compressedEvictionCount;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private long decompressedCacheSize;
            private long compressedCacheSize;
            private double decompressedHitRate;
            private double compressedHitRate;
            private long decompressedEvictionCount;
            private long compressedEvictionCount;

            public Builder decompressedCacheSize(long size) { this.decompressedCacheSize = size; return this; }
            public Builder compressedCacheSize(long size) { this.compressedCacheSize = size; return this; }
            public Builder decompressedHitRate(double rate) { this.decompressedHitRate = rate; return this; }
            public Builder compressedHitRate(double rate) { this.compressedHitRate = rate; return this; }
            public Builder decompressedEvictionCount(long count) { this.decompressedEvictionCount = count; return this; }
            public Builder compressedEvictionCount(long count) { this.compressedEvictionCount = count; return this; }

            public CacheStatistics build() { return new CacheStatistics(this); }
        }
    }
}
