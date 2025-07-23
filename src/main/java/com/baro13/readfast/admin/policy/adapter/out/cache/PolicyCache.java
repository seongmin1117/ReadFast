package com.baro13.readfast.admin.policy.adapter.out.cache;

import com.baro13.readfast.admin.policy.domain.model.DataRetentionPolicy;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PolicyCache {
    @Qualifier("policyCaffeineCache")
    private final Cache<Long, DataRetentionPolicy> cache;

    public DataRetentionPolicy get(Long id) {
        return cache.getIfPresent(id);
    }

    public void put(DataRetentionPolicy policy) {
        cache.put(policy.getPolicyId(), policy);
    }

    public void evict(Long id) {
        cache.invalidate(id);
    }
}
