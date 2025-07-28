package com.baro13.readfast.admin.authlog.adapter.out.db.querydsl;


import static com.baro13.readfast.admin.authlog.adapter.out.db.jpa.entity.QAuthLogEntity.authLogEntity;

import com.baro13.readfast.admin.authlog.adapter.in.controller.dto.AuthSearchCondition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import java.time.Instant;

public class AuthQueryConditionBuilder {
    
    private AuthQueryConditionBuilder() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static BooleanBuilder buildCondition(AuthSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(createdAtGoe(condition.getStartDate()));
        builder.and(createdAtLoe(condition.getEndDate()));
        builder.and(resultEq(condition.getResult()));
        builder.and(deviceEq(condition.getDevice()));
        builder.and(userIdEq(condition.getUserId()));
        builder.and(endpointEq(condition.getEndpoint()));
        return builder;
    }
    
    public static BooleanBuilder buildConditionWithCursor(AuthSearchCondition condition) {
        BooleanBuilder builder = buildCondition(condition);
        builder.and(cursorBefore(condition.getCursorDate(), condition.getCursorId()));
        return builder;
    }
    
    private static BooleanExpression createdAtGoe(Instant startDate) {
        return startDate == null ? null : authLogEntity.date.goe(startDate);
    }
    
    private static BooleanExpression createdAtLoe(Instant endDate) {
        return endDate == null ? null : authLogEntity.date.loe(endDate);
    }
    
    private static BooleanExpression resultEq(String result) {
        return result == null ? null : authLogEntity.result.eq(result);
    }
    
    private static BooleanExpression deviceEq(String device) {
        return device == null ? null : authLogEntity.device.eq(device);
    }
    
    private static BooleanExpression userIdEq(String userId) {
        return userId == null ? null : authLogEntity.userId.eq(userId);
    }
    
    private static BooleanExpression endpointEq(String endpoint) {
        return endpoint == null ? null : authLogEntity.endpoint.eq(endpoint);
    }
    
    private static BooleanExpression cursorBefore(Instant cursorDate, Long cursorId) {
        if (cursorDate == null || cursorId == null) return null;
        
        // 커서 기반 페이지네이션: 날짜 내림차순, ID 내림차순 정렬에서
        // 다음 페이지를 가져오기 위해서는 cursorDate보다 이전이거나, 
        // 같은 날짜면 cursorId보다 작은 것들을 가져와야 함
        return authLogEntity.date.lt(cursorDate)
            .or(authLogEntity.date.eq(cursorDate).and(authLogEntity.id.lt(cursorId)));
    }
}