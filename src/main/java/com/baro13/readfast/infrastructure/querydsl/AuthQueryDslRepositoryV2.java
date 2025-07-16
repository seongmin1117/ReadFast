package com.baro13.readfast.infrastructure.querydsl;

import static com.baro13.readfast.infrastructure.jpa.QAuthLogEntity.authLogEntity;

import com.baro13.readfast.controller.dto.AuthSearchCondition;
import com.baro13.readfast.domain.AuthLog;
import com.baro13.readfast.infrastructure.AuthLogMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuthQueryDslRepositoryV2 {
    private final JPAQueryFactory queryFactory;

    public Page<AuthLog> search(AuthSearchCondition condition, Pageable pageable){
        List<AuthLog> content = getContent(condition, pageable);
        return new PageImpl<>(content, pageable, -1);
    }

    public List<AuthLog> getContent(AuthSearchCondition condition, Pageable pageable) {
        return queryFactory.selectFrom(authLogEntity)
            .where(AuthQueryConditionBuilder.buildConditionWithCursor(condition))
            .limit(pageable.getPageSize())
            .orderBy(authLogEntity.date.desc(), authLogEntity.id.desc())
            .fetch()
            .stream().map(AuthLogMapper::toDomain).toList();
    }
}
