package com.baro13.readfast.infrastructure.policy;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "data.retention")
public class DataRetentionProperties {
    
    private int dbRetentionDays = 90;
    
    private int totalRetentionDays = 365;
    
    private int batchSize = 1000;
    
    private String archiveBasePath = "./archive-data";
    
    private String archiveFileFormat = "yyyy-MM-dd";
    
    private boolean enableArchiving = true;
    
    private boolean enableDataDeletion = true;
    
    private String cronExpression = "0 0 2 * * ?"; // 매일 새벽 2시
    
    // 압축 설정
    private boolean enableCompression = true;
    private String compressionFormat = "gzip"; // gzip, zip, none
    
    // 파일 형식 설정
    private String archiveDataFormat = "json"; // json, csv, parquet
    
    // SQLite 분석 DB 설정
    private boolean enableSqliteConversion = true;
    private boolean sqliteCleanupAfterConversion = false;
}