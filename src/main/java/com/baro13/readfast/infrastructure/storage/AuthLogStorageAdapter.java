package com.baro13.readfast.infrastructure.storage;

import com.baro13.readfast.application.port.AuthLogStorageReader;
import com.baro13.readfast.domain.AuthLog;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthLogStorageAdapter implements AuthLogStorageReader {
    
    private final StorageService storageService;
    
    @Override
    public void storeData(List<AuthLog> authLogs, LocalDate date) {
        storageService.storeData(authLogs, date);
    }
    
    @Override
    public List<AuthLog> retrieveData(LocalDate date) {
        return storageService.retrieveData(date);
    }
    
    @Override
    public List<AuthLog> retrieveDataByDateRange(LocalDate startDate, LocalDate endDate) {
        return storageService.retrieveDataByDateRange(startDate, endDate);
    }
    
    @Override
    public boolean dataExists(LocalDate date) {
        return storageService.dataExists(date);
    }
}