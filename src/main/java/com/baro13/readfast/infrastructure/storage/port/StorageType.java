package com.baro13.readfast.infrastructure.storage.port;

/**
 * 지원되는 스토리지 타입 정의
 */
public enum StorageType {
    LOCAL_FILE("로컬 파일"),
    SQLITE("SQLite 데이터베이스"),
    S3("Amazon S3"),
    AZURE_BLOB("Azure Blob Storage"),
    GCS("Google Cloud Storage"),
    POSTGRESQL("PostgreSQL");
    
    private final String description;
    
    StorageType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}