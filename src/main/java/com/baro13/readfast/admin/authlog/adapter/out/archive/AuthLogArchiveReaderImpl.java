package com.baro13.readfast.admin.authlog.adapter.out.archive;

import com.baro13.readfast.admin.authlog.adapter.out.archive.storage.DataStorage;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.admin.authlog.domain.port.AuthLogArchiveReader;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthLogArchiveReaderImpl implements AuthLogArchiveReader {
    
    private final DataStorageFactory dataStorageFactory;
    
    @Override
    public void storeData(List<AuthLog> authLogs, LocalDate date) {
        DataStorage storage = dataStorageFactory.resolve();
        storage.store(authLogs, date);
    }
    
    @Override
    public List<AuthLog> retrieveData(LocalDate date) {
        DataStorage storage = dataStorageFactory.resolve();
        return storage.retrieve(date);
    }
    
    @Override
    public List<AuthLog> retrieveDataByDateRange(LocalDate startDate, LocalDate endDate) {
        DataStorage storage = dataStorageFactory.resolve();
        return storage.retrieveByDateRange(startDate, endDate);
    }
    
    @Override
    public boolean dataExists(LocalDate date) {
        DataStorage storage = dataStorageFactory.resolve();
        return storage.exists(date);
    }
}