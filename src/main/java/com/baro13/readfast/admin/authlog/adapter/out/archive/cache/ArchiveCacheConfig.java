package com.baro13.readfast.admin.authlog.adapter.out.archive.cache;

import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Weigher;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ArchiveCacheConfig {

    /**
     * 고성능 아카이브 데이터 캐시 설정
     * - 메모리 기반 가중치 제한으로 OOM 방지
     * - LRU 방식으로 오래된 데이터 자동 제거
     * - 압축 해제된 데이터를 임시 캐싱하여 반복 접근 최적화
     */
    @Bean(name = "archiveCaffeineCache")
    public Cache<LocalDate, List<AuthLog>> archiveCaffeineCache() {
        return Caffeine.newBuilder()
            // 메모리 기반 제한: 최대 500MB 추정 (일평균 10K records = ~2MB per day)
            .maximumWeight(250_000_000L) // 250MB 상한선
            .weigher((Weigher<LocalDate, List<AuthLog>>) (key, value) -> {
                // 각 AuthLog를 약 200바이트로 추정하여 가중치 계산
                return value.size() * 200;
            })
            // TTL: 압축된 아카이브는 변경되지 않으므로 길게 설정
            .expireAfterWrite(2, TimeUnit.HOURS)
            // 접근 기반 TTL: 최근에 사용되지 않은 데이터는 빠르게 제거
            .expireAfterAccess(30, TimeUnit.MINUTES)
            // 제거 이벤트 로깅 (모니터링용)
            .removalListener((LocalDate key, List<AuthLog> value, RemovalCause cause) -> {
                if (value != null) {
                    log.debug("아카이브 캐시에서 제거됨: 날짜={}, 레코드={}, 원인={}", 
                             key, value.size(), cause);
                }
            })
            // 통계 활성화 (선택적 모니터링)
            .recordStats()
            .build();
    }

    /**
     * 압축된 바이트 데이터 캐시 - 메모리 효율성 극대화
     * 압축된 상태로 캐싱하여 메모리 사용량 최소화
     */
    @Bean(name = "compressedDataCache")
    public Cache<LocalDate, byte[]> compressedDataCache() {
        return Caffeine.newBuilder()
            // 압축된 데이터는 더 작으므로 많이 보관 가능
            .maximumWeight(100_000_000L) // 100MB
            .weigher((Weigher<LocalDate, byte[]>) (key, value) -> value.length)
            // 압축된 데이터는 더 오래 보관
            .expireAfterWrite(4, TimeUnit.HOURS)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .removalListener((LocalDate key, byte[] value, RemovalCause cause) -> {
                if (value != null) {
                    log.debug("압축 데이터 캐시에서 제거됨: 날짜={}, 크기={}bytes, 원인={}", 
                             key, value.length, cause);
                }
            })
            .recordStats()
            .build();
    }
}
