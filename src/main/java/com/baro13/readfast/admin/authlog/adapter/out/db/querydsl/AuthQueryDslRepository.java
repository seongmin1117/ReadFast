package com.baro13.readfast.admin.authlog.adapter.out.db.querydsl;


import static com.baro13.readfast.admin.authlog.adapter.out.db.jpa.entity.QAuthLogEntity.authLogEntity;

import com.baro13.readfast.admin.authlog.adapter.in.controller.dto.AuthSearchCondition;
import com.baro13.readfast.admin.authlog.adapter.out.db.jpa.mapper.AuthLogMapper;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
/**
 * 최적화된 인증 로그 검색 QueryDSL 리포지토리
 * 커서 기반 페이지네이션과 성능 최적화가 적용된 메인 리포지토리
 */
public class AuthQueryDslRepository {
    private final JPAQueryFactory queryFactory;

    public Page<AuthLog> search(AuthSearchCondition condition, Pageable pageable){
        List<AuthLog> content = getContent(condition, pageable);
        return new PageImpl<>(content, pageable, -1);
    }

    public List<AuthLog> getContent(AuthSearchCondition condition, Pageable pageable) {
        return queryFactory.selectFrom(authLogEntity)
            .where(AuthQueryConditionBuilder.buildConditionWithCursor(condition))
            .limit(pageable.getPageSize())
            .orderBy(getOrderSpecifiers(pageable))
            .fetch()
            .stream().map(AuthLogMapper::toDomain).toList();
    }
    
    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) {
        // 커서 기반 페이지네이션에서는 일관된 정렬이 중요하므로 
        // 기본적으로 date desc, id desc를 유지하되 추가 정렬 조건을 앞에 배치
        if (pageable.getSort().isEmpty()) {
            return new OrderSpecifier[]{
                authLogEntity.date.desc(),
                authLogEntity.id.desc()
            };
        }

        List<OrderSpecifier<?>> orderSpecifiers = pageable.getSort().stream()
            .map(this::getOrderSpecifier)
            .toList();
            
        // 커서 페이지네이션의 일관성을 위해 항상 date, id로 마지막 정렬
        orderSpecifiers = new java.util.ArrayList<>(orderSpecifiers);
        orderSpecifiers.add(authLogEntity.date.desc());
        orderSpecifiers.add(authLogEntity.id.desc());
        
        return orderSpecifiers.toArray(new OrderSpecifier[0]);
    }
    
    private OrderSpecifier<?> getOrderSpecifier(Sort.Order sortOrder) {
        Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;
        
        return switch (sortOrder.getProperty()) {
            case "id" -> new OrderSpecifier<>(direction, authLogEntity.id);
            case "userId" -> new OrderSpecifier<>(direction, authLogEntity.userId);
            case "result" -> new OrderSpecifier<>(direction, authLogEntity.result);
            case "device" -> new OrderSpecifier<>(direction, authLogEntity.device);
            case "endpoint" -> new OrderSpecifier<>(direction, authLogEntity.endpoint);
            case "date" -> new OrderSpecifier<>(direction, authLogEntity.date);
            default -> new OrderSpecifier<>(direction, authLogEntity.date);
        };
    }
}
