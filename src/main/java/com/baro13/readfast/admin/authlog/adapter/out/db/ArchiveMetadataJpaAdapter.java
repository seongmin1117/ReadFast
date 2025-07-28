package com.baro13.readfast.admin.authlog.adapter.out.db;

import com.baro13.readfast.admin.authlog.adapter.out.db.jpa.ArchiveMetadataJpaRepository;
import com.baro13.readfast.admin.authlog.adapter.out.db.jpa.mapper.ArchiveMetadataMapper;
import com.baro13.readfast.admin.authlog.domain.model.ArchiveMetadata;
import com.baro13.readfast.admin.authlog.domain.port.ArchiveMetadataRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 아카이브 메타데이터 JPA 어댑터
 * 헥사고날 아키텍처의 2차 어댑터로서 JPA를 통한 데이터베이스 접근을 담당
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ArchiveMetadataJpaAdapter implements ArchiveMetadataRepository {

    private final ArchiveMetadataJpaRepository jpaRepository;
    private final ArchiveMetadataMapper mapper;

    @Override
    @Transactional(readOnly = false) // Master DB에 저장 보장
    public ArchiveMetadata save(ArchiveMetadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("저장할 ArchiveMetadata가 null입니다");
        }

        try {
            var entity = mapper.toEntity(metadata);
            var savedEntity = jpaRepository.save(entity);
            var result = mapper.toDomain(savedEntity);

            log.info("아카이브 메타데이터 Master DB 저장 완료. ID: {}, 파일경로: {}, 스토리지타입: {}", 
                    result.getId(), result.getFilePath(), result.getStorageType());

            return result;

        } catch (Exception e) {
            log.error("아카이브 메타데이터 Master DB 저장 실패. 파일경로: {}, 오류: {}", 
                    metadata.getFilePath(), e.getMessage(), e);
            throw new RuntimeException("아카이브 메타데이터 Master DB 저장 중 오류 발생", e);
        }
    }

    @Override
    public Optional<ArchiveMetadata> findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("조회할 ID가 null입니다");
        }

        try {
            return jpaRepository.findById(id)
                .filter(entity -> !entity.isDeleted()) // soft delete 체크
                .map(mapper::toDomain);

        } catch (Exception e) {
            log.error("아카이브 메타데이터 ID 조회 실패. ID: {}", id, e);
            throw new RuntimeException("아카이브 메타데이터 조회 중 오류 발생", e);
        }
    }

    @Override
    public List<ArchiveMetadata> findByDateRange(Instant startDate, Instant endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("시작일 또는 종료일이 null입니다");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일이 종료일보다 이후일 수 없습니다");
        }

        try {
            return jpaRepository.findByDateRange(startDate, endDate)
                .stream()
                .map(mapper::toDomain)
                .toList();

        } catch (Exception e) {
            log.error("아카이브 메타데이터 날짜 범위 조회 실패. 기간: {} ~ {}", startDate, endDate, e);
            throw new RuntimeException("아카이브 메타데이터 날짜 범위 조회 중 오류 발생", e);
        }
    }

    @Override
    public List<ArchiveMetadata> findByExactDateRange(Instant startDate, Instant endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("시작일 또는 종료일이 null입니다");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일이 종료일보다 이후일 수 없습니다");
        }

        try {
            return jpaRepository.findByExactDateRange(startDate, endDate)
                .stream()
                .map(mapper::toDomain)
                .toList();

        } catch (Exception e) {
            log.error("아카이브 메타데이터 정확한 날짜 범위 조회 실패. 기간: {} ~ {}", startDate, endDate, e);
            throw new RuntimeException("아카이브 메타데이터 정확한 날짜 범위 조회 중 오류 발생", e);
        }
    }

    @Override
    public List<ArchiveMetadata> findByStorageType(String storageType) {
        if (storageType == null || storageType.trim().isEmpty()) {
            throw new IllegalArgumentException("스토리지 타입이 null이거나 빈 문자열입니다");
        }

        try {
            return jpaRepository.findByStorageTypeAndDeletedFalseOrderByArchivedAtDesc(storageType)
                .stream()
                .map(mapper::toDomain)
                .toList();

        } catch (Exception e) {
            log.error("아카이브 메타데이터 스토리지 타입 조회 실패. 타입: {}", storageType, e);
            throw new RuntimeException("아카이브 메타데이터 스토리지 타입 조회 중 오류 발생", e);
        }
    }

    @Override
    public Optional<ArchiveMetadata> findByFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("파일 경로가 null이거나 빈 문자열입니다");
        }

        try {
            return jpaRepository.findByFilePathAndDeletedFalse(filePath)
                .map(mapper::toDomain);

        } catch (Exception e) {
            log.error("아카이브 메타데이터 파일 경로 조회 실패. 경로: {}", filePath, e);
            throw new RuntimeException("아카이브 메타데이터 파일 경로 조회 중 오류 발생", e);
        }
    }

    /**
     * 삭제되지 않은 모든 아카이브 메타데이터 조회 (최신순)
     * 
     * @return 아카이브 메타데이터 목록
     */
    public List<ArchiveMetadata> findAll() {
        try {
            return jpaRepository.findByDeletedFalseOrderByArchivedAtDesc()
                .stream()
                .map(mapper::toDomain)
                .toList();

        } catch (Exception e) {
            log.error("전체 아카이브 메타데이터 조회 실패", e);
            throw new RuntimeException("전체 아카이브 메타데이터 조회 중 오류 발생", e);
        }
    }

}