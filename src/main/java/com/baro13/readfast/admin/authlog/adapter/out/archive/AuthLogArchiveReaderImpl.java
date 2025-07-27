package com.baro13.readfast.admin.authlog.adapter.out.archive;

import com.baro13.readfast.admin.authlog.adapter.out.archive.compression.CompressionFactory;
import com.baro13.readfast.admin.authlog.adapter.out.archive.storage.Storage;
import com.baro13.readfast.admin.authlog.adapter.out.archive.storage.StorageFactory;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.admin.authlog.domain.port.AuthLogArchiveReader;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthLogArchiveReaderImpl implements AuthLogArchiveReader {
    
    private final StorageFactory storageFactory;
    private final CompressionFactory compressionFactory;

    @Override
    public void storeData(List<AuthLog> authLogs, LocalDate date) {
        Storage storage = storageFactory.resolve();
        storage.store(authLogs, date);
    }
    
    @Override
    public List<AuthLog> retrieveData(LocalDate date) {
        Storage storage = storageFactory.resolve();
        return storage.retrieve(date);
    }
    
    @Override
    public List<AuthLog> retrieveDataByDateRange(LocalDate startDate, LocalDate endDate) {
        Storage storage = storageFactory.resolve();
        return storage.retrieveByDateRange(startDate, endDate);
    }
    
    @Override
    public boolean dataExists(LocalDate date) {
        Storage storage = storageFactory.resolve();
        return storage.exists(date);
    }
}