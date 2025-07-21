package com.baro13.readfast.application.port;

import com.baro13.readfast.domain.AuthLog;
import java.time.LocalDate;
import java.util.List;

public interface AuthLogStorageReader {
    
    void storeData(List<AuthLog> authLogs, LocalDate date);
    
    List<AuthLog> retrieveData(LocalDate date);
    
    List<AuthLog> retrieveDataByDateRange(LocalDate startDate, LocalDate endDate);
    
    boolean dataExists(LocalDate date);
}