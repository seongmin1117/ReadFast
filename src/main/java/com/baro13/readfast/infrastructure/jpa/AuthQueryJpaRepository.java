package com.baro13.readfast.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthQueryJpaRepository extends JpaRepository<AuthLogEntity,Long> {

}
