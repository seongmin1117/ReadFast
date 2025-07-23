package com.baro13.readfast.admin.policy.adapter.out.jpa;

import com.baro13.readfast.admin.policy.domain.model.vo.ArchivingStrategy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 데이터 보관 정책 JPA 엔티티 (MVP 간소화 버전)
 */
@Entity
@Table(name = "data_retention_policy")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataRetentionPolicyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long policyId;

    @Column(nullable = false)
    private Integer dbRetentionDays;

    @Column(nullable = false)
    private Integer totalRetentionDays;

    @Column(nullable = false)
    private Integer batchSize;

    @Column(nullable = false)
    private Boolean enableDataDeletion;

    @Column(nullable = false, length = 500)
    private String archiveBasePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArchivingStrategy.ArchiveFormat archiveFormat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArchivingStrategy.CompressionType compressionType;


    public void updateRetentionRule(Integer dbRetentionDays, Integer totalRetentionDays,
        Integer batchSize, Boolean enableDataDeletion) {
        this.dbRetentionDays = dbRetentionDays;
        this.totalRetentionDays = totalRetentionDays;
        this.batchSize = batchSize;
        this.enableDataDeletion = enableDataDeletion;
    }

    public void updateArchivingStrategy(String archiveBasePath,
        ArchivingStrategy.ArchiveFormat archiveFormat,
        ArchivingStrategy.CompressionType compressionType
    ) {
        this.archiveBasePath = archiveBasePath;
        this.archiveFormat = archiveFormat;
        this.compressionType = compressionType;
    }

}