package com.baro13.readfast.admin.policy.application.in.dto;

public record UpdatePolicyCommand(
    Long policyId,
    int dbRetentionDays,
    int totalRetentionDays,
    int batchSize,
    boolean enableDataDeletion,
    String archiveBasePath,
    String archiveFormat,
    String compressionType) {

}