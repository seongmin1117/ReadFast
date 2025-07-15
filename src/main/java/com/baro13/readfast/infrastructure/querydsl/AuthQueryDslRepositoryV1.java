package com.baro13.readfast.infrastructure.querydsl;

import static com.baro13.readfast.infrastructure.jpa.QAuthLogEntity.authLogEntity;

import com.baro13.readfast.controller.dto.AuthSearchCondition;
import com.baro13.readfast.domain.AuthLog;
import com.baro13.readfast.infrastructure.AuthLogMapper;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
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
            .where(getCondition(condition))
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
            .where(getCondition(condition))
            .fetchOne();
    }

    private BooleanBuilder getCondition(AuthSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(createdAtGoe(condition.getStartDate()));
        builder.and(createdAtLoe(condition.getEndDate()));
        builder.and(resultEq(condition.getResult()));
        builder.and(deviceEq(condition.getDevice()));
        builder.and(userIdEq(condition.getUserId()));
        builder.and(endpointEq(condition.getEndpoint()));
        return builder;
    }

    private BooleanExpression createdAtGoe(Instant startDate) {
        return startDate == null ? null : authLogEntity.date.goe(startDate);
    }
    private BooleanExpression createdAtLoe(Instant endDate) {
        return endDate == null ? null : authLogEntity.date.loe(endDate);
    }
    private BooleanExpression resultEq(String result){
        return result == null ? null : authLogEntity.result.eq(result);
    }
    private BooleanExpression deviceEq(String device){
        return device == null ? null : authLogEntity.device.eq(device);
    }
    private BooleanExpression userIdEq(String userId){
        return userId == null ? null : authLogEntity.userId.eq(userId);
    }
    private BooleanExpression endpointEq(String endpoint){
        return endpoint == null ? null : authLogEntity.endpoint.eq(endpoint);
    }


    private OrderSpecifier<?> getOrderSpecifier(Pageable pageable) {
        Sort.Order sortOrder = pageable.getSort().stream().findFirst()
            .orElse(Sort.Order.desc("date"));

        Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;

        return new OrderSpecifier<>(direction, authLogEntity.date);
    }
}
