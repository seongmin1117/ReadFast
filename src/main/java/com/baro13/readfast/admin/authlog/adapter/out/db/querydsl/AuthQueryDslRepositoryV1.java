package com.baro13.readfast.adapter.out.authlog.db.querydsl;

import static com.baro13.readfast.adapter.out.db.jpa.QAuthLogEntity.authLogEntity;

import com.baro13.readfast.adapter.in.controller.authlog.dto.AuthSearchCondition;
import com.baro13.readfast.adapter.out.authlog.db.jpa.mapper.AuthLogMapper;
import com.baro13.readfast.domain.model.AuthLog;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuthQueryDslRepositoryV1 {
    private final JPAQueryFactory queryFactory;

    public Page<AuthLog> search(AuthSearchCondition condition, Pageable pageable){
        List<AuthLog> content = getContent(condition, pageable);
        Long count = getCount(condition);
        return new PageImpl<>(content, pageable, count);
    }

    public List<AuthLog> getContent(AuthSearchCondition condition, Pageable pageable) {
        return queryFactory.selectFrom(authLogEntity)
            .where(AuthQueryConditionBuilder.buildCondition(condition))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(getOrderSpecifier(pageable))
            .fetch()
            .stream().map(AuthLogMapper::toDomain).toList();
    }

    private Long getCount(AuthSearchCondition condition) {
        return queryFactory
            .select(authLogEntity.count())
            .from(authLogEntity)
            .where(AuthQueryConditionBuilder.buildCondition(condition))
            .fetchOne();
    }


    private OrderSpecifier<?> getOrderSpecifier(Pageable pageable) {
        Sort.Order sortOrder = pageable.getSort().stream().findFirst()
            .orElse(Sort.Order.desc("date"));

        Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;
        
        return switch (sortOrder.getProperty()) {
            case "id" -> new OrderSpecifier<>(direction, authLogEntity.id);
            case "userId" -> new OrderSpecifier<>(direction, authLogEntity.userId);
            case "result" -> new OrderSpecifier<>(direction, authLogEntity.result);
            case "device" -> new OrderSpecifier<>(direction, authLogEntity.device);
            case "endpoint" -> new OrderSpecifier<>(direction, authLogEntity.endpoint);
            default -> new OrderSpecifier<>(direction, authLogEntity.date);
        };
    }
    
    /**
     * 지정된 날짜보다 오래된 인증 로그 조회 (아카이빙용)
     */
    public List<AuthLog> findOlderThan(LocalDateTime cutoffDate, int batchSize) {
        var cutoffInstant = cutoffDate.toInstant(ZoneOffset.UTC);
        return queryFactory.selectFrom(authLogEntity)
                .where(authLogEntity.date.lt(cutoffInstant))
                .orderBy(authLogEntity.date.asc())
                .limit(batchSize)
                .fetch()
                .stream()
                .map(AuthLogMapper::toDomain)
                .toList();
    }
}
