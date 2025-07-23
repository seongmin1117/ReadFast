package com.baro13.readfast.admin.authlog.domain.port;

import com.baro13.readfast.admin.policy.domain.model.DataRetentionPolicy;

public interface DataRetentionPolicyProvider {
    DataRetentionPolicy getCurrentPolicy();
}
