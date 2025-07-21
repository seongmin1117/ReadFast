package com.baro13.readfast.infrastructure.storage;

import com.baro13.readfast.domain.AuthLog;
import com.baro13.readfast.infrastructure.storage.port.DataStorage;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {
    
    private final List<DataStorage> storages;
    private final @Qualifier("taskExecutor") Executor taskExecutor;
    
    public void storeData(List<AuthLog> authLogs, LocalDate date) {
        // 병렬 스토리지 저장으로 성능 개선
        List<CompletableFuture<Void>> futures = storages.stream()
            .map(storage -> CompletableFuture.runAsync(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    storage.store(authLogs, date);
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("{}에 {}일자 데이터 저장 성공 ({}ms)", 
                        storage.getStorageType(), date, duration);
                } catch (Exception e) {
                    log.error("{}에 {}일자 데이터 저장 실패", 
                        storage.getStorageType(), date, e);
                }
            }, taskExecutor))
            .toList();
        
        // 모든 스토리지 저장 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .join();
    }
    
    public List<AuthLog> retrieveData(LocalDate date) {
        for (DataStorage storage : storages) {
            try {
                if (storage.exists(date)) {
                    List<AuthLog> data = storage.retrieve(date);
                    log.info("{}에서 {}일자 데이터 조회 성공", storage.getStorageType(), date);
                    return data;
                }
            } catch (Exception e) {
                log.error("{}에서 {}일자 데이터 조회 실패", storage.getStorageType(), date, e);
            }
        }
        log.warn("{}일자 데이터를 찾을 수 없음", date);
        return List.of();
    }
    
    public List<AuthLog> retrieveDataByDateRange(LocalDate startDate, LocalDate endDate) {
        return storages.stream()
            .flatMap(storage -> {
                try {
                    return storage.retrieveByDateRange(startDate, endDate).stream();
                } catch (Exception e) {
                    log.error("{}에서 {} ~ {} 기간 데이터 조회 실패", 
                        storage.getStorageType(), startDate, endDate, e);
                    return Stream.<AuthLog>of();
                }
            })
            .collect(Collectors.toList());
    }
    
    public boolean dataExists(LocalDate date) {
        return storages.stream()
            .anyMatch(storage -> {
                try {
                    return storage.exists(date);
                } catch (Exception e) {
                    log.error("{}에서 {}일자 데이터 존재 여부 확인 실패", 
                        storage.getStorageType(), date, e);
                    return false;
                }
            });
    }
}