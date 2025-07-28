package com.baro13.readfast.admin.authlog.adapter.out.db.jpa.mapper;

import com.baro13.readfast.admin.authlog.adapter.out.db.jpa.entity.ArchiveMetadataEntity;
import com.baro13.readfast.admin.authlog.domain.model.ArchiveMetadata;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * ArchiveMetadata와 ArchiveMetadataEntity 간의 매핑을 담당하는 매퍼
 * 도메인 모델과 JPA 엔티티 간의 변환 로직을 제공
 */
@Component
public class ArchiveMetadataMapper {

    /**
     * 도메인 모델을 JPA 엔티티로 변환
     * 
     * @param archiveMetadata 도메인 모델
     * @return JPA 엔티티
     * @throws IllegalArgumentException archiveMetadata가 null인 경우
     */
    public ArchiveMetadataEntity toEntity(ArchiveMetadata archiveMetadata) {
        if (archiveMetadata == null) {
            throw new IllegalArgumentException("ArchiveMetadata는 null일 수 없습니다");
        }

        return new ArchiveMetadataEntity(
            archiveMetadata.getId(),
            archiveMetadata.getStartDate(),
            archiveMetadata.getEndDate(),
            archiveMetadata.getStorageType(),
            archiveMetadata.getCompressionType(),
            archiveMetadata.getFilePath(),
            archiveMetadata.getFileSizeBytes(),
            archiveMetadata.getRecordsCount(),
            archiveMetadata.getArchivedAt(),
            false // 새로 생성시 삭제 상태는 false
        );
    }

    /**
     * JPA 엔티티를 도메인 모델로 변환
     * 
     * @param entity JPA 엔티티
     * @return 도메인 모델
     * @throws IllegalArgumentException entity가 null인 경우
     */
    public ArchiveMetadata toDomain(ArchiveMetadataEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("ArchiveMetadataEntity는 null일 수 없습니다");
        }

        return ArchiveMetadata.of(
            entity.getId(),
            entity.getStartDate(),
            entity.getEndDate(),
            entity.getStorageType(),
            entity.getCompressionType(),
            entity.getFilePath(),
            entity.getFileSizeBytes(),
            entity.getRecordCount(),
            entity.getArchivedAt(),
            entity.isDeleted()
        );
    }

    /**
     * 배치 작업 결과로부터 ArchiveMetadata 생성
     * 
     * @param storageType 스토리지 타입
     * @param filePath 파일 경로
     * @param fileSizeBytes 파일 크기
     * @param startDate 아카이브 시작 날짜
     * @param endDate 아카이브 종료 날짜
     * @return ArchiveMetadata 도메인 객체
     */
    public ArchiveMetadata createFromBatchResult(String storageType, String filePath, 
                                               Long fileSizeBytes, Instant startDate, Instant endDate, String compressionType, Integer recordCount) {
        return ArchiveMetadata.of(
            null, // ID는 저장 시 자동 생성
            startDate,
            endDate,
            storageType,
            compressionType,
            filePath,
            fileSizeBytes,
            recordCount,
            Instant.now(), // 현재 시간으로 아카이브 생성 시간 설정
            false
        );
    }
}