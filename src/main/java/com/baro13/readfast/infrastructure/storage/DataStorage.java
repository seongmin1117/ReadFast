package com.baro13.readfast.infrastructure.storage;

import com.baro13.readfast.domain.AuthLog;
import java.time.LocalDate;
import java.util.List;

public interface DataStorage {
    
    void store(List<AuthLog> authLogs, LocalDate date);
    
    List<AuthLog> retrieve(LocalDate date);
    
    List<AuthLog> retrieveByDateRange(LocalDate startDate, LocalDate endDate);
    
    boolean exists(LocalDate date);
    
    void delete(LocalDate date);
    
    StorageType getStorageType();
    
    enum StorageType {
        LOCAL_FILE, S3, AZURE_BLOB, GCS, SQLITE
    }
}