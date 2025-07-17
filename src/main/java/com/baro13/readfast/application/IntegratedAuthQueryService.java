package com.baro13.readfast.application;

import com.baro13.readfast.application.out.AuthQueryRepository;
import com.baro13.readfast.controller.dto.AuthSearchCondition;
import com.baro13.readfast.domain.AuthLog;
import com.baro13.readfast.infrastructure.batch.config.DataRetentionProperties;
import com.baro13.readfast.infrastructure.storage.StorageService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegratedAuthQueryService {
    
    private final AuthQueryRepository authQueryRepository;
    private final StorageService storageService;
    private final DataRetentionProperties properties;
    
    public Page<AuthLog> searchIntegratedV1(AuthSearchCondition condition) {
        Instant dbCutoffDate = Instant.now().minus(properties.getDbRetentionDays(), ChronoUnit.DAYS);
        
        // DB에서 조회할 수 있는 기간인지 확인
        if (condition.getStartDate() != null && condition.getStartDate().isBefore(dbCutoffDate)) {
            log.info("통합 조회 V1 실행: 스토리지와 DB에서 조회");
            return searchFromStorageAndDb(condition, dbCutoffDate);
        }
        
        // DB에서만 조회
        log.info("통합 조회 V1 실행: DB에서만 조회");
        return authQueryRepository.searchV1(condition, createPageable(condition));
    }
    
    public Page<AuthLog> searchIntegratedV2(AuthSearchCondition condition) {
        Instant dbCutoffDate = Instant.now().minus(properties.getDbRetentionDays(), ChronoUnit.DAYS);
        
        // DB에서 조회할 수 있는 기간인지 확인
        if (condition.getStartDate() != null && condition.getStartDate().isBefore(dbCutoffDate)) {
            log.info("통합 조회 V2 실행: 스토리지와 DB에서 조회");
            return searchFromStorageAndDb(condition, dbCutoffDate);
        }
        
        // DB에서만 조회
        log.info("통합 조회 V2 실행: DB에서만 조회");
        return authQueryRepository.searchV2(condition, createPageable(condition));
    }
    
    private Page<AuthLog> searchFromStorageAndDb(AuthSearchCondition condition, Instant dbCutoffDate) {
        List<AuthLog> allResults = new ArrayList<>();
        
        // 스토리지에서 조회
        if (condition.getStartDate() != null) {
            LocalDate startDate = condition.getStartDate().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate endDate = condition.getEndDate() != null 
                ? condition.getEndDate().atZone(ZoneId.systemDefault()).toLocalDate()
                : dbCutoffDate.atZone(ZoneId.systemDefault()).toLocalDate();
            
            List<AuthLog> storageResults = storageService.retrieveDataByDateRange(startDate, endDate);
            List<AuthLog> filteredStorageResults = filterStorageResults(storageResults, condition);
            allResults.addAll(filteredStorageResults);
            log.info("스토리지에서 {}개 레코드 조회 완료", filteredStorageResults.size());
        }
        
        // DB에서 조회 (필요한 경우)
        if (condition.getEndDate() == null || condition.getEndDate().isAfter(dbCutoffDate)) {
            AuthSearchCondition dbCondition = createDbCondition(condition, dbCutoffDate);
            Page<AuthLog> dbResults = authQueryRepository.searchV1(dbCondition, createPageable(condition));
            allResults.addAll(dbResults.getContent());
            log.info("DB에서 {}개 레코드 조회 완료", dbResults.getContent().size());
        }
        
        // 결과 필터링 및 정렬
        List<AuthLog> filteredResults = allResults.stream()
            .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
            .collect(Collectors.toList());
        
        // 페이징 처리
        Page<AuthLog> result = applyPagination(filteredResults, createPageable(condition));
        log.info("통합 조회 완료: 총 {}개 레코드 중 {}개 반환", filteredResults.size(), result.getContent().size());
        return result;
    }
    
    private List<AuthLog> filterStorageResults(List<AuthLog> results, AuthSearchCondition condition) {
        return results.stream()
            .filter(log -> matchesCondition(log, condition))
            .collect(Collectors.toList());
    }
    
    private boolean matchesCondition(AuthLog log, AuthSearchCondition condition) {
        if (condition.getStartDate() != null && log.getDate().isBefore(condition.getStartDate())) {
            return false;
        }
        if (condition.getEndDate() != null && log.getDate().isAfter(condition.getEndDate())) {
            return false;
        }
        if (condition.getResult() != null && !condition.getResult().equals(log.getResult())) {
            return false;
        }
        if (condition.getDevice() != null && !condition.getDevice().equals(log.getDevice())) {
            return false;
        }
        if (condition.getUserId() != null && !condition.getUserId().equals(log.getUserId())) {
            return false;
        }
        if (condition.getEndpoint() != null && !condition.getEndpoint().equals(log.getEndpoint())) {
            return false;
        }
        return true;
    }
    
    private AuthSearchCondition createDbCondition(AuthSearchCondition original, Instant dbCutoffDate) {
        return new AuthSearchCondition(
            dbCutoffDate, // startDate를 DB cutoff date로 설정
            original.getEndDate(),
            original.getDevice(),
            original.getUserId(),
            original.getResult(),
            original.getEndpoint(),
            original.getPage(),
            original.getSize(),
            original.getSortBy(),
            original.getDirection(),
            original.getCursorId(),
            original.getCursorDate()
        );
    }
    
    private Pageable createPageable(AuthSearchCondition condition) {
        return org.springframework.data.domain.PageRequest.of(
            condition.getPage(),
            condition.getSize(),
            org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.fromString(condition.getDirection()),
                condition.getSortBy()
            )
        );
    }
    
    private Page<AuthLog> applyPagination(List<AuthLog> results, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), results.size());
        
        if (start > results.size()) {
            return new PageImpl<>(List.of(), pageable, results.size());
        }
        
        List<AuthLog> pageContent = results.subList(start, end);
        return new PageImpl<>(pageContent, pageable, results.size());
    }
}