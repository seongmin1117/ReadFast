package com.baro13.readfast.admin.policy.adapter.in.controller.dto;

import com.baro13.readfast.admin.policy.application.in.dto.UpdatePolicyCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdatePolicyRequest(
    @NotNull(message = "DB 보관 일수는 필수입니다")
    @Min(value = 1, message = "DB 보관 일수는 1일 이상이어야 합니다")
    Integer dbRetentionDays,

    @NotNull(message = "전체 보관 일수는 필수입니다")
    @Min(value = 1, message = "전체 보관 일수는 1일 이상이어야 합니다")
    Integer totalRetentionDays,

    @NotNull(message = "배치 크기는 필수입니다")
    @Min(value = 100, message = "배치 크기는 100 이상이어야 합니다")
    Integer batchSize,

    @NotNull(message = "데이터 삭제 여부는 필수입니다")
    Boolean enableDataDeletion,

    @NotBlank(message = "아카이브 경로는 필수입니다")
    String archiveBasePath,

    @NotBlank(message = "아카이브 형식은 필수입니다")
    String archiveFormat,

    @NotBlank(message = "압축 형식은 필수입니다")
    String compressionType
) {

    public UpdatePolicyCommand toCommand(Long policyId) {
        return new UpdatePolicyCommand(policyId,
            dbRetentionDays, totalRetentionDays, batchSize, enableDataDeletion, archiveBasePath,
            archiveFormat,compressionType);
    }
}
