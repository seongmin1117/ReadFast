package com.baro13.readfast.admin.policy.domain.model;

import com.baro13.readfast.admin.policy.domain.model.vo.ArchivingStrategy;
import com.baro13.readfast.admin.policy.domain.model.vo.RetentionRule;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class DataRetentionPolicy {
    private final Long policyId;
    private final RetentionRule retentionRule;
    private final ArchivingStrategy archivingStrategy;

    public DataRetentionPolicy update(RetentionRule retentionRule, ArchivingStrategy archivingStrategy) {
        return this.toBuilder()
            .retentionRule(retentionRule)
            .archivingStrategy(archivingStrategy)
            .build();
    }
}