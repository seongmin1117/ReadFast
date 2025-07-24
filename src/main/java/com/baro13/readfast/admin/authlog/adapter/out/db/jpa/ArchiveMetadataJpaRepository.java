package com.baro13.readfast.admin.authlog.adapter.out.db.jpa;

import com.baro13.readfast.admin.authlog.adapter.out.db.jpa.entity.ArchiveMetadataEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 아카이브 메타데이터 JPA Repository
 */
public interface ArchiveMetadataJpaRepository extends JpaRepository<ArchiveMetadataEntity, Long> {

    /**
     * 특정 기간의 아카이브 메타데이터 조회 (겹치는 기간 포함)
     * 
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 해당 기간의 메타데이터 목록 (삭제되지 않은 것만)
     */
    @Query("SELECT a FROM ArchiveMetadataEntity a WHERE " +
           "((a.startDate <= :endDate AND a.endDate >= :startDate) OR " +
           "(a.startDate >= :startDate AND a.startDate <= :endDate) OR " +
           "(a.endDate >= :startDate AND a.endDate <= :endDate)) AND " +
           "a.deleted = false " +
           "ORDER BY a.archivedAt DESC")
    List<ArchiveMetadataEntity> findByDateRange(@Param("startDate") Instant startDate, 
                                               @Param("endDate") Instant endDate);

    /**
     * 정확한 기간 일치하는 아카이브 메타데이터 조회 (중복 체크용)
     * 
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 해당 기간의 메타데이터 목록 (삭제되지 않은 것만)
     */
    @Query("SELECT a FROM ArchiveMetadataEntity a WHERE " +
           "a.startDate = :startDate AND a.endDate = :endDate AND " +
           "a.deleted = false " +
           "ORDER BY a.archivedAt DESC")
    List<ArchiveMetadataEntity> findByExactDateRange(@Param("startDate") Instant startDate, 
                                                    @Param("endDate") Instant endDate);

    /**
     * 스토리지 타입으로 조회 (삭제되지 않은 것만)
     * 
     * @param storageType 스토리지 타입
     * @return 해당 스토리지 타입의 메타데이터 목록
     */
    List<ArchiveMetadataEntity> findByStorageTypeAndDeletedFalseOrderByArchivedAtDesc(String storageType);

    /**
     * 파일 경로로 조회 (삭제되지 않은 것만)
     * 
     * @param filePath 파일 경로
     * @return 메타데이터 (존재하지 않으면 empty)
     */
    Optional<ArchiveMetadataEntity> findByFilePathAndDeletedFalse(String filePath);

    /**
     * 삭제되지 않은 모든 메타데이터 조회 (최신순)
     * 
     * @return 메타데이터 목록
     */
    List<ArchiveMetadataEntity> findByDeletedFalseOrderByArchivedAtDesc();
}