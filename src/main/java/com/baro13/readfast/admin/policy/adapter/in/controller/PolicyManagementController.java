package com.baro13.readfast.admin.policy.adapter.in.controller;

import com.baro13.readfast.admin.policy.adapter.in.controller.dto.UpdatePolicyRequest;
import com.baro13.readfast.admin.policy.application.in.PolicyManagementService;
import com.baro13.readfast.admin.policy.domain.model.DataRetentionPolicy;
import com.baro13.readfast.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyManagementController {

    private final PolicyManagementService policyManagementService;

    @PutMapping("/{policyId}")
    public ResponseEntity<ApiResponse<DataRetentionPolicy>> updatePolicy(
        @PathVariable Long policyId, @Valid @RequestBody UpdatePolicyRequest request) {
        log.info("정책 업데이트 요청");
        var result = policyManagementService.updatePolicy(request.toCommand(policyId));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{policyId}")
    public ResponseEntity<ApiResponse<DataRetentionPolicy>> getPolicy(@PathVariable Long policyId) {
        log.info("정책 조회 요청");
        var result = policyManagementService.getPolicy(policyId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

}