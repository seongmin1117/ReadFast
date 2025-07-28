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

        if (condition.getStartDate() != null && condition.getStartDate().isBefore(dbCutoffDate)) {
            log.info("통합 조회 실행: 스토리지와 DB에서 조회 (커서 기반)");
            return searchFromStorageAndDb(condition, dbCutoffDate);
        }
        
        // DB에서만 조회 (커서 기반)
        log.info("통합 조회 실행: DB에서만 조회 (커서 기반)");
        return authLogDbReader.search(condition, createPageable(condition));
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
            log.info("스토리지에서 {}개 레코드 조회 완료 (V2)", filteredStorageResults.size());
        }
        
        // DB에서 조회
        if (condition.getEndDate() == null || condition.getEndDate().isAfter(dbCutoffDate)) {
            AuthSearchCondition dbCondition = createDbCondition(condition, dbCutoffDate);
            Page<AuthLog> dbResults = authLogDbReader.search(dbCondition, createPageable(condition));
            allResults.addAll(dbResults.getContent());
            log.info("DB에서 {}개 레코드 조회 완료", dbResults.getContent().size());
        }
        
        // 전체 결과를 날짜 내림차순, ID 내림차순으로 정렬
        List<AuthLog> sortedResults = allResults.parallelStream()
            .sorted((a, b) -> {
                int dateComparison = b.getDate().compareTo(a.getDate());
                if (dateComparison == 0) {
                    return Long.compare(b.getId(), a.getId()); // ID도 내림차순
                }
                return dateComparison;
            })
            .collect(Collectors.toList());
        
        // 커서 기반 필터링 및 페이징을 한번에 처리
        Page<AuthLog> result = applyCursorBasedPagination(sortedResults, condition);
        log.info("통합 조회 완료: 총 {}개 레코드 중 {}개 반환 (커서: {}/{})",
                sortedResults.size(), result.getContent().size(), 
                condition.getCursorId(), condition.getCursorDate());
        return result;
    }

    /**
     * 커서 기반 페이지네이션 적용
     * hasNext를 정확하게 계산하기 위해 pageSize + 1개를 조회한 후 처리
     */
    private Page<AuthLog> applyCursorBasedPagination(List<AuthLog> allResults, AuthSearchCondition condition) {
        int pageSize = condition.getSize();
        
        // 커서 필터링 적용
        List<AuthLog> filteredResults = applyCursorFilter(allResults, condition);
        
        // hasNext 계산: pageSize + 1개를 요청해서 더 있는지 확인
        boolean hasNext = filteredResults.size() > pageSize;
        
        // 실제 반환할 콘텐츠 (pageSize개만)
        List<AuthLog> pageContent = hasNext 
            ? filteredResults.subList(0, pageSize)
            : filteredResults;
        
        // 커스텀 Page 구현체로 hasNext 정보 포함
        return new CursorBasedPage<>(pageContent, createPageable(condition), hasNext);
    }
    
    // 커서 기반 페이지네이션을 위한 커스텀 Page 구현체
    private static class CursorBasedPage<T> implements Page<T> {
        private final List<T> content;
        private final Pageable pageable;
        private final boolean hasNext;
        
        public CursorBasedPage(List<T> content, Pageable pageable, boolean hasNext) {
            this.content = content;
            this.pageable = pageable;
            this.hasNext = hasNext;
        }
        
        @Override
        public List<T> getContent() { return content; }
        
        @Override
        public Pageable getPageable() { return pageable; }
        
        @Override
        public boolean hasNext() { return hasNext; }
        
        @Override
        public boolean isLast() { return !hasNext; }
        
        @Override
        public boolean isFirst() { return true; } // 커서 기반에서는 항상 첫 페이지 취급
        
        @Override
        public int getNumber() { return 0; } // 커서 기반에서는 페이지 번호 무의미
        
        @Override
        public int getSize() { return pageable.getPageSize(); }
        
        @Override
        public int getNumberOfElements() { return content.size(); }
        
        @Override
        public long getTotalElements() { return -1; } // 커서 기반에서는 총 개수 알 수 없음
        
        @Override
        public int getTotalPages() { return -1; } // 커서 기반에서는 총 페이지 수 알 수 없음
        
        @Override
        public boolean hasPrevious() { return false; } // 커서 기반에서는 이전 페이지 개념 없음
        
        @Override
        public Pageable nextPageable() { return Pageable.unpaged(); }
        
        @Override
        public Pageable previousPageable() { return Pageable.unpaged(); }
        
        @Override
        public boolean isEmpty() { return content.isEmpty(); }
        
        @Override
        public boolean hasContent() { return !content.isEmpty(); }
        
        @Override
        public java.util.Iterator<T> iterator() { return content.iterator(); }
        
        @Override
        public org.springframework.data.domain.Sort getSort() { 
            return pageable.getSort(); 
        }
        
        @Override
        public <U> Page<U> map(java.util.function.Function<? super T, ? extends U> converter) {
            List<U> convertedContent = content.stream().map(converter).collect(java.util.stream.Collectors.toList());
            return new CursorBasedPage<>(convertedContent, pageable, hasNext);
        }
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
    
    
    /**
     * 커서 기반 필터링 적용
     * 날짜 내림차순, ID 내림차순 정렬에서 커서 이후의 데이터를 가져옴
     */
    private List<AuthLog> applyCursorFilter(List<AuthLog> results, AuthSearchCondition condition) {
        if (condition.getCursorDate() == null || condition.getCursorId() == null) {
            return results;
        }
        
        return results.stream()
            .filter(log -> {
                // 정렬이 날짜 내림차순, ID 내림차순이므로
                // 커서 날짜보다 이전이거나, 같은 날짜면 커서 ID보다 작은 것만 포함
                int dateComparison = log.getDate().compareTo(condition.getCursorDate());
                if (dateComparison < 0) {
                    return true; // 날짜가 이전이면 포함
                } else if (dateComparison == 0) {
                    return log.getId() < condition.getCursorId(); // 같은 날짜면 ID가 작은 것만
                }
                return false; // 날짜가 이후면 제외
            })
            .collect(Collectors.toList());
    }
}