package com.baro13.readfast.admin.policy.domain.model.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
public class ArchivingStrategy {
    private final String archiveBasePath;
    private final ArchiveFormat archiveFormat;
    private final CompressionType compressionType;

    @Getter
    @RequiredArgsConstructor
    public enum CompressionType {
        NONE(""),
        GZIP(".gz");

        private final String extension;
    }

    @Getter
    @RequiredArgsConstructor
    public enum ArchiveFormat {
        CSV(".csv"),
        JSON(".json"),
        SQLITE(".sqlite");

        private final String extension;
    }

    public static ArchivingStrategy create(String archiveBasePath,
        ArchiveFormat archiveFormat,
        CompressionType compressionType) {
        return ArchivingStrategy.builder()
            .archiveBasePath(archiveBasePath)
            .archiveFormat(archiveFormat)
            .compressionType(compressionType)
            .build();
    }

    public String getFullExtension() {
        return archiveFormat.getExtension() + compressionType.getExtension();
    }
}