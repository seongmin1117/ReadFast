package com.baro13.readfast.adapter.out.authlog.archive;

import com.baro13.readfast.domain.model.AuthLog;
import com.baro13.readfast.domain.port.AuthLogArchiveReader;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthLogArchiveReaderImpl implements AuthLogArchiveReader {
    
    private final AuthLogArchiveStorage authLogArchiveStorage;
    
    @Override
    public void storeData(List<AuthLog> authLogs, LocalDate date) {
        authLogArchiveStorage.storeData(authLogs, date);
    }
    
    @Override
    public List<AuthLog> retrieveData(LocalDate date) {
        return authLogArchiveStorage.retrieveData(date);
    }
    
    @Override
    public List<AuthLog> retrieveDataByDateRange(LocalDate startDate, LocalDate endDate) {
        return authLogArchiveStorage.retrieveDataByDateRange(startDate, endDate);
    }
    
    @Override
    public boolean dataExists(LocalDate date) {
        return authLogArchiveStorage.dataExists(date);
    }
}