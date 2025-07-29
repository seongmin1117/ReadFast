package com.baro13.readfast.admin.authlog.adapter.out.db.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j

@Configuration
public class AuthLogCacheConfig {

    @Bean(name = "authLogStatsCache")
    public Cache<LocalDate, AuthLogStats> authLogStatsCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(365)
            .build();
    }
}
