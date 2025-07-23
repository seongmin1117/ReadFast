package com.baro13.readfast.admin.policy.adapter.out.jpa;

import com.baro13.readfast.admin.policy.domain.model.DataRetentionPolicy;
import com.baro13.readfast.admin.policy.domain.model.vo.ArchivingStrategy;
import com.baro13.readfast.admin.policy.domain.model.vo.RetentionRule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataRetentionPolicyMapper {

    public static DataRetentionPolicy toDomain(DataRetentionPolicyEntity entity) {
        if (entity == null) {
            return null;
        }

        var retentionRule = RetentionRule.create(
            entity.getDbRetentionDays(),
            entity.getTotalRetentionDays(),
            entity.getBatchSize(),
            entity.getEnableDataDeletion()
        );

        var archivingStrategy = ArchivingStrategy.create(
            entity.getArchiveBasePath(),
            entity.getArchiveFormat(),
            entity.getCompressionType()
        );

        return DataRetentionPolicy.builder()
            .policyId(entity.getPolicyId())
            .retentionRule(retentionRule)
            .archivingStrategy(archivingStrategy)
            .build();
    }

    public static DataRetentionPolicyEntity toEntity(DataRetentionPolicy domain) {
        if (domain == null) {
            return null;
        }

        return DataRetentionPolicyEntity.builder()
            .policyId(domain.getPolicyId())
            .dbRetentionDays(domain.getRetentionRule().getDbRetentionDays())
            .totalRetentionDays(domain.getRetentionRule().getTotalRetentionDays())
            .batchSize(domain.getRetentionRule().getBatchSize())
            .enableDataDeletion(domain.getRetentionRule().isEnableDataDeletion())
            .archiveBasePath(domain.getArchivingStrategy().getArchiveBasePath())
            .compressionType(domain.getArchivingStrategy().getCompressionType())
            .build();
    }

    public static void updateEntity(DataRetentionPolicyEntity entity, DataRetentionPolicy domain) {
        if (entity == null || domain == null) {
            return;
        }

        entity.updateRetentionRule(
            domain.getRetentionRule().getDbRetentionDays(),
            domain.getRetentionRule().getTotalRetentionDays(),
            domain.getRetentionRule().getBatchSize(),
            domain.getRetentionRule().isEnableDataDeletion()
        );

        entity.updateArchivingStrategy(
            domain.getArchivingStrategy().getArchiveBasePath(),
            domain.getArchivingStrategy().getArchiveFormat(),
            domain.getArchivingStrategy().getCompressionType()
        );

    }
}