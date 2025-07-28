package com.baro13.readfast.admin.authlog.domain.model;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ArchiveMetadata {

    private Long id;
    private Instant startDate;
    private Instant endDate;
    private String storageType;
    private String compressionType;
    private String filePath;
    private Long fileSizeBytes;
    private Integer recordsCount;
    private Instant archivedAt;
    private Boolean deleted;

    public static ArchiveMetadata of(Long id, Instant startDate, Instant endDate,
        String storageType, String compressionType, String filePath, Long fileSizeBytes,
        Integer recordsCount, Instant archivedAt, Boolean deleted) {
        return ArchiveMetadata.builder()
            .id(id)
            .startDate(startDate)
            .endDate(endDate)
            .storageType(storageType)
            .compressionType(compressionType)
            .filePath(filePath)
            .fileSizeBytes(fileSizeBytes)
            .recordsCount(recordsCount)
            .archivedAt(archivedAt)
            .deleted(deleted != null && deleted)
            .build();
    }
}
