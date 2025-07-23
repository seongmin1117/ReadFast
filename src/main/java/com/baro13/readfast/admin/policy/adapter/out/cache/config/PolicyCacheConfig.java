package com.baro13.readfast.admin.policy.adapter.out.cache.config;

import com.baro13.readfast.admin.policy.domain.model.DataRetentionPolicy;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PolicyCacheConfig {

    @Bean("policyCaffeineCache")
    public Cache<Long, DataRetentionPolicy> policyCache() {
        return Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(Duration.ofMinutes(10)) // 필요 시 조정
            .build();
    }
}
