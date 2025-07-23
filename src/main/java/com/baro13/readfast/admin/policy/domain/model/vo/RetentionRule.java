package com.baro13.readfast.admin.policy.domain.model.vo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RetentionRule {
    private final int dbRetentionDays;
    private final int totalRetentionDays;
    private final int batchSize;
    private final boolean enableDataDeletion;

    public static RetentionRule create(int dbRetentionDays, int totalRetentionDays,
                                     int batchSize, boolean enableDataDeletion) {
        return RetentionRule.builder()
                .dbRetentionDays(dbRetentionDays)
                .totalRetentionDays(totalRetentionDays)
                .batchSize(batchSize)
                .enableDataDeletion(enableDataDeletion)
                .build();
    }
}