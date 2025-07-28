package com.baro13.readfast.admin.authlog.adapter.out.db.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "archive_metadata")
public class ArchiveMetadataEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant startDate;       // 아카이브 시작일
    private Instant endDate;         // 아카이브 종료일

    @Column(nullable = false)
    private String storageType;      // 예: sqlite, csv, parquet

    @Column(nullable = false)
    private String compressionType;

    @Column(nullable = false)
    private String filePath;         // 파일 위치 (로컬경로 또는 S3 Key 등)

    private Long fileSizeBytes;      // 파일 크기 (선택)

    private Integer recordCount;

    private Instant archivedAt;      // 아카이브 생성 시각

    private boolean deleted;         // 삭제 여부 (soft delete)
}
