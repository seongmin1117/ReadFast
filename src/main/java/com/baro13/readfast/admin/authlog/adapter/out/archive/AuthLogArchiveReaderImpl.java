package com.baro13.readfast.admin.authlog.adapter.out.archive;

import com.baro13.readfast.admin.authlog.adapter.out.archive.cache.ArchiveCache;
import com.baro13.readfast.admin.authlog.adapter.out.archive.storage.Storage;
import com.baro13.readfast.admin.authlog.adapter.out.archive.storage.StorageFactory;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.admin.authlog.domain.port.AuthLogArchiveReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
        Optional<List<AuthLog>> cached = archiveCache.getDecompressed(date);
        if (cached.isPresent() && !cached.get().isEmpty()) {
            log.debug("캐시에서 단일 날짜 데이터 조회: 날짜={}, 레코드={}", date, cached.get().size());
            return cached.get();
        }
        
        // 캐시에 없으면 storage에서 읽고, 캐시에 저장
        List<AuthLog> fromStorage = loadFromStorageAndCache(date);
        
        log.debug("스토리지에서 단일 날짜 데이터 조회 및 캐싱: 날짜={}, 레코드={}", date, fromStorage.size());
        return fromStorage;
    }
    
    @Override
    public List<AuthLog> retrieveDataByDateRange(LocalDate startDate, LocalDate endDate) {
        List<AuthLog> allLogs = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<AuthLog> dailyLogs = retrieveDataForSingleDate(date);
            allLogs.addAll(dailyLogs);
        }
        
        log.debug("날짜 범위 데이터 조회 완료: {} ~ {}, 총 레코드={}", startDate, endDate, allLogs.size());
        return allLogs;
    }
    
    @Override
    public boolean dataExists(LocalDate date) {
        Storage storage = storageFactory.resolve();
        return storage.exists(date);
    }
    
    /**
     * 단일 날짜에 대한 데이터 조회 (캐시 확인 포함)
     */
    private List<AuthLog> retrieveDataForSingleDate(LocalDate date) {
        Optional<List<AuthLog>> cached = archiveCache.getDecompressed(date);
        if (cached.isPresent() && !cached.get().isEmpty()) {
            return cached.get();
        }
        
        return loadFromStorageAndCache(date);
    }
    
    /**
     * 스토리지에서 데이터를 로드하고 캐시에 저장
     */
    private List<AuthLog> loadFromStorageAndCache(LocalDate date) {
        try {
            Storage storage = storageFactory.resolve();
            List<AuthLog> fromStorage = storage.retrieve(date);
            
            // 빈 리스트는 캐시하지 않음 (메모리 절약)
            if (!fromStorage.isEmpty()) {
                archiveCache.putDecompressed(date, fromStorage);
            }
            
            return fromStorage;
        } catch (Exception e) {
            log.error("스토리지에서 데이터 로드 실패: 날짜={}", date, e);
            return Collections.emptyList();
        }
    }
}