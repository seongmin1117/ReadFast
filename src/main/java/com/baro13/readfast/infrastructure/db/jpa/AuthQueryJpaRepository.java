package com.baro13.readfast.infrastructure.db.jpa;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthQueryJpaRepository extends JpaRepository<AuthLogEntity,Long> {
    
    @Query("SELECT a FROM AuthLogEntity a WHERE a.date < :cutoffDate ORDER BY a.date ASC")
    Page<AuthLogEntity> findByDateBefore(@Param("cutoffDate") Instant cutoffDate, Pageable pageable);
    
    @Query("SELECT a FROM AuthLogEntity a WHERE a.date >= :startDate AND a.date < :endDate ORDER BY a.date ASC")
    List<AuthLogEntity> findByDateBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    long countByDateBefore(Instant cutoffDate);
}
