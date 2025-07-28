package com.baro13.readfast.admin.authlog.adapter.out.archive;

import com.baro13.readfast.admin.authlog.adapter.out.archive.cache.ArchiveCache;
import com.baro13.readfast.admin.authlog.adapter.out.archive.storage.Storage;
import com.baro13.readfast.admin.authlog.adapter.out.archive.storage.StorageFactory;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.admin.authlog.domain.port.AuthLogArchiveReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthLogArchiveReaderImpl implements AuthLogArchiveReader {
    
    private final StorageFactory storageFactory;
    private final ArchiveCache archiveCache;

    @Override
    public void storeData(List<AuthLog> authLogs, LocalDate date) {
        Storage storage = storageFactory.resolve();
        storage.store(authLogs, date);
    }
    
    @Override
    public List<AuthLog> retrieveData(LocalDate date) {
        // 캐시에서 먼저 확인
        List<AuthLog> cached = archiveCache.get(date);
        if (cached != null && !cached.isEmpty()) {
            log.debug("캐시에서 단일 날짜 데이터 조회: 날짜={}, 레코드={}", date, cached.size());
            return cached;
        }
        
        // 캐시에 없으면 storage에서 읽고, 캐시에 저장
        Storage storage = storageFactory.resolve();
        List<AuthLog> fromStorage = storage.retrieve(date);
        archiveCache.put(date, fromStorage);
        
        log.debug("스토리지에서 단일 날짜 데이터 조회 및 캐싱: 날짜={}, 레코드={}", date, fromStorage.size());
        return fromStorage;
    }
    
    @Override
    public List<AuthLog> retrieveDataByDateRange(LocalDate startDate, LocalDate endDate) {
        List<AuthLog> allLogs = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<AuthLog> cached = archiveCache.get(date);
            if (cached != null && !cached.isEmpty()) {
                allLogs.addAll(cached);
            } else {
                // 캐시에 없으면 storage에서 읽고, 캐시에 저장
                List<AuthLog> fromStorage = storageFactory.resolve().retrieve(date);
                archiveCache.put(date, fromStorage);
                allLogs.addAll(fromStorage);
            }
        }
        return allLogs;
    }
    
    @Override
    public boolean dataExists(LocalDate date) {
        Storage storage = storageFactory.resolve();
        return storage.exists(date);
    }
}