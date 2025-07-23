package com.baro13.readfast.admin.authlog.domain.port;

import com.baro13.readfast.admin.authlog.domain.model.ArchiveMetadata;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 아카이브 메타데이터 저장소 포트
 * 배치 작업 후 아카이브된 파일의 메타데이터를 관리하는 도메인 포트
 */
public interface ArchiveMetadataRepository {

    /**
     * 아카이브 메타데이터 저장
     * 
     * @param metadata 저장할 메타데이터
     * @return 저장된 메타데이터 (ID 포함)
     * @throws IllegalArgumentException metadata가 null인 경우
     * @throws RuntimeException 저장 중 오류가 발생한 경우
     */
    ArchiveMetadata save(ArchiveMetadata metadata);

    /**
     * 아카이브 메타데이터 조회
     * 
     * @param id 조회할 메타데이터 ID
     * @return 메타데이터 (존재하지 않으면 empty)
     * @throws IllegalArgumentException id가 null인 경우
     */
    Optional<ArchiveMetadata> findById(Long id);

    /**
     * 특정 기간의 아카이브 메타데이터 조회
     * 
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 해당 기간의 메타데이터 목록
     * @throws IllegalArgumentException startDate 또는 endDate가 null인 경우
     */
    List<ArchiveMetadata> findByDateRange(Instant startDate, Instant endDate);

    /**
     * 특정 스토리지 타입의 아카이브 메타데이터 조회
     * 
     * @param storageType 스토리지 타입 (예: "sqlite", "csv")
     * @return 해당 스토리지 타입의 메타데이터 목록
     * @throws IllegalArgumentException storageType이 null이거나 빈 문자열인 경우
     */
    List<ArchiveMetadata> findByStorageType(String storageType);

    /**
     * 특정 파일 경로의 아카이브 메타데이터 조회
     * 
     * @param filePath 파일 경로
     * @return 메타데이터 (존재하지 않으면 empty)
     * @throws IllegalArgumentException filePath가 null이거나 빈 문자열인 경우
     */
    Optional<ArchiveMetadata> findByFilePath(String filePath);
}