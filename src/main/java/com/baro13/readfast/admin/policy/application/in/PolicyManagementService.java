package com.baro13.readfast.admin.policy.application.in;

import com.baro13.readfast.admin.policy.adapter.out.cache.PolicyCache;
import com.baro13.readfast.admin.policy.application.in.dto.UpdatePolicyCommand;
import com.baro13.readfast.admin.policy.domain.model.DataRetentionPolicy;
import com.baro13.readfast.admin.policy.domain.model.vo.ArchivingStrategy;
import com.baro13.readfast.admin.policy.domain.model.vo.RetentionRule;
import com.baro13.readfast.admin.policy.domain.port.DataRetentionPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PolicyManagementService {
    
    private final DataRetentionPolicyRepository policyRepository;
    private final PolicyCache policyCache;

    public DataRetentionPolicy updatePolicy(UpdatePolicyCommand command) {
        log.info("정책 업데이트 시작");

            var policy = getPolicy(command.policyId());

            // 새로운 규칙들 생성
            var newRetentionRule = RetentionRule.create(
                    command.dbRetentionDays(),
                    command.totalRetentionDays(),
                    command.batchSize(),
                    command.enableDataDeletion()
            );
            
            var newArchivingStrategy = ArchivingStrategy.create(
                    command.archiveBasePath(),
                    policy.getArchivingStrategy().getArchiveFormat(),
                    policy.getArchivingStrategy().getCompressionType()
            );

            // 정책 업데이트
            var updatedPolicy = policy.update(
                    newRetentionRule,
                    newArchivingStrategy
            );

            // 정책 저장
            var savedPolicy = policyRepository.save(updatedPolicy);

            // 캐시 반영
            policyCache.put(savedPolicy);

            log.info("정책 업데이트 완료 - PolicyId: {}", savedPolicy.getPolicyId());
            return savedPolicy;
    }

    @Transactional(readOnly = true)
    public DataRetentionPolicy getPolicy(Long policyId) {
        return policyRepository.findById(policyId).orElseThrow();
    }

}