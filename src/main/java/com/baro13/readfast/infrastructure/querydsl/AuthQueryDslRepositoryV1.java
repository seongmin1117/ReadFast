package com.baro13.readfast.infrastructure.querydsl;

import static com.baro13.readfast.infrastructure.jpa.QAuthLogEntity.authLogEntity;

import com.baro13.readfast.controller.dto.AuthSearchCondition;
import com.baro13.readfast.domain.AuthLog;
import com.baro13.readfast.infrastructure.AuthLogMapper;
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
}
