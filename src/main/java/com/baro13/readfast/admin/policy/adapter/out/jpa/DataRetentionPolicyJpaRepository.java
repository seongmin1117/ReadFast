package com.baro13.readfast.admin.policy.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DataRetentionPolicyJpaRepository extends JpaRepository<DataRetentionPolicyEntity, Long> {

}