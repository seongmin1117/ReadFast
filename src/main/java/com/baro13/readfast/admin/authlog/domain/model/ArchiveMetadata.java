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
    private String filePath;
    private Long fileSizeBytes;
    private Instant archivedAt;

    public static ArchiveMetadata of(Long id, Instant startDate, Instant endDate, String storageType, String filePath, Long fileSizeBytes, Instant archivedAt) {
        return ArchiveMetadata.builder()
            .id(id)
            .startDate(startDate)
            .endDate(endDate)
            .storageType(storageType)
            .filePath(filePath)
            .fileSizeBytes(fileSizeBytes)
            .archivedAt(archivedAt)
            .build();
    }
}
