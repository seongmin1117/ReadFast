package com.baro13.readfast.admin.policy.adapter.out;

import com.baro13.readfast.admin.policy.adapter.out.jpa.DataRetentionPolicyJpaRepository;
import com.baro13.readfast.admin.policy.adapter.out.jpa.DataRetentionPolicyMapper;
import com.baro13.readfast.admin.policy.domain.model.DataRetentionPolicy;
import com.baro13.readfast.admin.policy.domain.port.DataRetentionPolicyRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional
public class DataRetentionPolicyRepositoryImpl implements DataRetentionPolicyRepository {
    
    private final DataRetentionPolicyJpaRepository jpaRepository;
    
    @Override
    public DataRetentionPolicy save(DataRetentionPolicy policy) {
        var existingEntity = jpaRepository.findById(policy.getPolicyId());

        if (existingEntity.isPresent()) {
            // 기존 엔티티 업데이트
            var entity = existingEntity.get();
            DataRetentionPolicyMapper.updateEntity(entity, policy);
            var savedEntity = jpaRepository.save(entity);
            return DataRetentionPolicyMapper.toDomain(savedEntity);
        } else {
            // 새 엔티티 생성
            var entity = DataRetentionPolicyMapper.toEntity(policy);
            var savedEntity = jpaRepository.save(entity);
            return DataRetentionPolicyMapper.toDomain(savedEntity);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<DataRetentionPolicy> findById(Long policyId) {
        return jpaRepository.findById(policyId)
                .map(DataRetentionPolicyMapper::toDomain);
    }

}