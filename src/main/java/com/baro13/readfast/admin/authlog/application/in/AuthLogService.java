package com.baro13.readfast.admin.authlog.application.in;

import com.baro13.readfast.admin.authlog.adapter.in.controller.dto.AuthSearchCondition;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.admin.authlog.domain.port.AuthLogArchiveReader;
import com.baro13.readfast.admin.authlog.domain.port.AuthLogDbReader;
import com.baro13.readfast.admin.authlog.domain.port.DataRetentionPolicyProvider;
import com.baro13.readfast.admin.policy.domain.model.DataRetentionPolicy;
import com.baro13.readfast.global.common.TimeZoneConstants;
import java.time.Instant;
import java.time.LocalDate;
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
public class AuthLogService {
    
    private final AuthLogDbReader authLogDbReader;
    private final AuthLogArchiveReader authLogArchiveReader;
    private final DataRetentionPolicyProvider dataRetentionPolicyProvider;

    public Page<AuthLog> search(AuthSearchCondition condition) {
        DataRetentionPolicy policy = dataRetentionPolicyProvider.getCurrentPolicy();
        int dbRetentionDays = policy.getRetentionRule().getDbRetentionDays();
        Instant dbCutoffDate = Instant.now().minus(dbRetentionDays, ChronoUnit.DAYS);
        
        // DB에서 조회할 수 있는 기간인지 확인
        if (condition.getStartDate() != null && condition.getStartDate().isBefore(dbCutoffDate)) {
            log.info("통합 조회 V3 실행: 스토리지와 DB에서 조회");
            return searchFromStorageAndDb(condition, dbCutoffDate);
        }
        
        // DB에서만 조회
        log.info("통합 조회 실행: DB에서만 조회");
        return authLogDbReader.search(condition, createPageable(condition));
    }

    public Page<AuthLog> searchV2(AuthSearchCondition condition) {
        DataRetentionPolicy policy = dataRetentionPolicyProvider.getCurrentPolicy();
        int dbRetentionDays = policy.getRetentionRule().getDbRetentionDays();
        Instant dbCutoffDate = Instant.now().minus(dbRetentionDays, ChronoUnit.DAYS);
        
        // V2는 커서 기반 페이지네이션 지원
        if (condition.getStartDate() != null && condition.getStartDate().isBefore(dbCutoffDate)) {
            log.info("통합 조회 V3-V2 실행: 스토리지와 DB에서 조회 (커서 기반)");
            return searchFromStorageAndDbV2(condition, dbCutoffDate);
        }
        
        // DB에서만 조회 (커서 기반)
        log.info("통합 조회 V2 실행: DB에서만 조회 (커서 기반)");
        return authLogDbReader.search(condition, createPageable(condition));
    }

    private Page<AuthLog> searchFromStorageAndDbV2(AuthSearchCondition condition, Instant dbCutoffDate) {
        // V2 방식으로 커서 기반 조회 (간소화된 버전)
        List<AuthLog> allResults = new ArrayList<>();
        
        // 스토리지에서 조회 (V2 방식 적용)
        if (condition.getStartDate() != null) {
            LocalDate startDate = condition.getStartDate().atZone(TimeZoneConstants.APPLICATION_ZONE).toLocalDate();
            LocalDate endDate = condition.getEndDate() != null 
                ? condition.getEndDate().atZone(TimeZoneConstants.APPLICATION_ZONE).toLocalDate()
                : dbCutoffDate.atZone(TimeZoneConstants.APPLICATION_ZONE).toLocalDate();
            
            List<AuthLog> storageResults = authLogArchiveReader.retrieveDataByDateRange(startDate, endDate);
            List<AuthLog> filteredStorageResults = filterStorageResults(storageResults, condition);
            allResults.addAll(filteredStorageResults);
            log.info("스토리지에서 {}개 레코드 조회 완료 (V2)", filteredStorageResults.size());
        }
        
        // DB에서 조회 (V2 방식)
        if (condition.getEndDate() == null || condition.getEndDate().isAfter(dbCutoffDate)) {
            AuthSearchCondition dbCondition = createDbCondition(condition, dbCutoffDate);
            Page<AuthLog> dbResults = authLogDbReader.search(dbCondition, createPageable(condition));
            allResults.addAll(dbResults.getContent());
            log.info("DB에서 {}개 레코드 조회 완료 (V2)", dbResults.getContent().size());
        }
        
        // 커서 기반 정렬 및 필터링
        List<AuthLog> filteredResults = allResults.parallelStream()
            .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
            .collect(Collectors.toList());
        
        // V2 스타일 페이징 (hasNext 정보 포함)
        Page<AuthLog> result = applyPaginationV2(filteredResults, createPageable(condition));
        log.info("통합 조회 V2 완료: 총 {}개 레코드 중 {}개 반환", filteredResults.size(), result.getContent().size());
        return result;
    }

    private Page<AuthLog> applyPaginationV2(List<AuthLog> results, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), results.size());
        
        if (start > results.size()) {
            return new PageImpl<>(List.of(), pageable, results.size());
        }
        
        List<AuthLog> pageContent = results.subList(start, end);
        
        // V2는 hasNext 정보만 제공 (totalElements는 추정치 또는 -1)
        boolean hasNext = end < results.size();
        long totalElements = hasNext ? -1 : results.size(); // -1은 더 있음을 의미
        
        return new PageImpl<>(pageContent, pageable, totalElements);
    }
    
    private Page<AuthLog> searchFromStorageAndDb(AuthSearchCondition condition, Instant dbCutoffDate) {
        List<AuthLog> allResults = new ArrayList<>();
        
        // 스토리지에서 조회
        if (condition.getStartDate() != null) {
            LocalDate startDate = condition.getStartDate().atZone(TimeZoneConstants.APPLICATION_ZONE).toLocalDate();
            LocalDate endDate = condition.getEndDate() != null 
                ? condition.getEndDate().atZone(TimeZoneConstants.APPLICATION_ZONE).toLocalDate()
                : dbCutoffDate.atZone(TimeZoneConstants.APPLICATION_ZONE).toLocalDate();
            
            List<AuthLog> storageResults = authLogArchiveReader.retrieveDataByDateRange(startDate, endDate);
            List<AuthLog> filteredStorageResults = filterStorageResults(storageResults, condition);
            allResults.addAll(filteredStorageResults);
            log.info("스토리지에서 {}개 레코드 조회 완료", filteredStorageResults.size());
        }
        
        // DB에서 조회 (필요한 경우)
        if (condition.getEndDate() == null || condition.getEndDate().isAfter(dbCutoffDate)) {
            AuthSearchCondition dbCondition = createDbCondition(condition, dbCutoffDate);
            Page<AuthLog> dbResults = authLogDbReader.search(dbCondition, createPageable(condition));
            allResults.addAll(dbResults.getContent());
            log.info("DB에서 {}개 레코드 조회 완료", dbResults.getContent().size());
        }
        
        // 결과 필터링 및 정렬 (병렬 처리로 성능 최적화)
        List<AuthLog> filteredResults = allResults.parallelStream()
            .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
            .collect(Collectors.toList());
        
        // 페이징 처리
        Page<AuthLog> result = applyPagination(filteredResults, createPageable(condition));
        log.info("통합 조회 완료: 총 {}개 레코드 중 {}개 반환", filteredResults.size(), result.getContent().size());
        return result;
    }
    
    private List<AuthLog> filterStorageResults(List<AuthLog> results, AuthSearchCondition condition) {
        // 대용량 데이터의 경우 병렬 스트림 사용으로 성능 최적화
        return results.parallelStream()
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