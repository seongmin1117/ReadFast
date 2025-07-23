package com.baro13.readfast.admin.policy.domain.port;

import com.baro13.readfast.admin.policy.domain.model.DataRetentionPolicy;
import java.util.Optional;

public interface DataRetentionPolicyRepository {
    DataRetentionPolicy save(DataRetentionPolicy policy);
    Optional<DataRetentionPolicy> findById(Long policyId);
}