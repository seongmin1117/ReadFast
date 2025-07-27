package com.baro13.readfast.admin.authlog.adapter.out.db.querydsl;

import static com.baro13.readfast.admin.authlog.adapter.out.db.jpa.entity.QAuthLogEntity.authLogEntity;

import com.baro13.readfast.admin.authlog.adapter.out.db.jpa.mapper.AuthLogMapper;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 아카이빙 전용 QueryDSL 리포지토리
 * 커서 기반 아카이빙 조회만 담당
 */
@Repository
@RequiredArgsConstructor
public class AuthArchiveRepository {
    private final JPAQueryFactory queryFactory;
    
    /**
     * 커서 기반으로 지정된 날짜보다 오래된 인증 로그 조회 (아카이빙용)
     * ID 기반 커서를 사용하여 중복 없이 순차적으로 데이터를 처리
     * 
     * @param cutoffDate 기준 날짜
     * @param batchSize 배치 크기
     * @param lastProcessedId 마지막 처리된 ID (커서), null이면 처음부터 시작
     * @return 조회된 인증 로그 리스트 (ID 순으로 정렬)
     */
    public List<AuthLog> findOlderThan(LocalDateTime cutoffDate, int batchSize, Long lastProcessedId) {
        var cutoffInstant = cutoffDate.toInstant(ZoneOffset.UTC);
        
        var query = queryFactory.selectFrom(authLogEntity)
                .where(authLogEntity.date.lt(cutoffInstant));
        
        // 커서가 있는 경우 해당 ID보다 큰 것만 조회
        if (lastProcessedId != null) {
            query = query.where(authLogEntity.id.gt(lastProcessedId));
        }
        
        return query
                .orderBy(authLogEntity.id.asc()) // ID 순으로 정렬하여 일관된 순서 보장
                .limit(batchSize)
                .fetch()
                .stream()
                .map(AuthLogMapper::toDomain)
                .toList();
    }
    
    /**
     * 동시성 안전성을 위한 스냅샷 기반 조회
     * REPEATABLE READ 격리 수준에서 데이터 일관성 보장
     */
    @Transactional(readOnly = true)
    public List<AuthLog> findOlderThanWithSnapshot(LocalDateTime cutoffDate, int limit, Long lastProcessedId) {
        var cutoffInstant = cutoffDate.toInstant(ZoneOffset.UTC);
        
        var query = queryFactory.selectFrom(authLogEntity)
                .where(authLogEntity.date.lt(cutoffInstant));
        
        if (lastProcessedId != null) {
            query = query.where(authLogEntity.id.gt(lastProcessedId));
        }
        
        return query
                .orderBy(authLogEntity.id.asc())
                .limit(limit)
                .fetch()
                .stream()
                .map(AuthLogMapper::toDomain)
                .toList();
    }
    
    /**
     * 특정 ID 목록의 데이터 삭제
     */
    @Transactional
    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        
        return (int) queryFactory.delete(authLogEntity)
                .where(authLogEntity.id.in(ids))
                .execute();
    }
    
    /**
     * 지정된 날짜보다 오래된 데이터 개수 조회
     */
    @Transactional(readOnly = true)
    public long countOlderThan(LocalDateTime cutoffDate) {
        var cutoffInstant = cutoffDate.toInstant(ZoneOffset.UTC);
        
        return queryFactory.selectFrom(authLogEntity)
                .where(authLogEntity.date.lt(cutoffInstant))
                .fetchCount();
    }
}
