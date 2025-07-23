package com.baro13.readfast.admin.policy.adapter.out;

import com.baro13.readfast.admin.authlog.domain.port.DataRetentionPolicyProvider;
import com.baro13.readfast.admin.policy.adapter.out.cache.PolicyCache;
import com.baro13.readfast.admin.policy.domain.model.DataRetentionPolicy;
import com.baro13.readfast.admin.policy.domain.port.DataRetentionPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataRetentionPolicyProviderImpl implements DataRetentionPolicyProvider {

    private final DataRetentionPolicyRepository policyRepository;
    private final PolicyCache policyCache;

    private static final Long ACTIVE_POLICY_ID = 1L;

    @Override
    public DataRetentionPolicy getCurrentPolicy() {

        var cached = policyCache.get(ACTIVE_POLICY_ID);
        if (cached != null) {
            return cached;
        }

        var policy = policyRepository.findById(ACTIVE_POLICY_ID)
            .orElseThrow(() -> new IllegalStateException("활성화된 정책을 찾을 수 없습니다."));

        policyCache.put(policy);
        return policy;
    }
}
