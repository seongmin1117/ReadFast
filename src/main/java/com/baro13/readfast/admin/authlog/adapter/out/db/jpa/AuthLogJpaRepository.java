package com.baro13.readfast.admin.authlog.adapter.out.db.jpa;

import com.baro13.readfast.admin.authlog.adapter.out.db.jpa.entity.AuthLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthLogJpaRepository extends JpaRepository<AuthLogEntity,Long> {

}
