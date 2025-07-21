package com.baro13.readfast.infrastructure.db;

import com.baro13.readfast.application.port.AuthLogDbReader;
import com.baro13.readfast.controller.dto.AuthSearchCondition;
import com.baro13.readfast.domain.AuthLog;
import com.baro13.readfast.global.logging.LogQueryTime;
import com.baro13.readfast.infrastructure.db.querydsl.AuthQueryDslRepositoryV1;
import com.baro13.readfast.infrastructure.db.querydsl.AuthQueryDslRepositoryV2;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuthLogDbReaderImpl implements AuthLogDbReader {
    private final AuthQueryDslRepositoryV1 authQueryDslRepositoryV1;
    private final AuthQueryDslRepositoryV2 authQueryDslRepositoryV2;

    @Override
    @LogQueryTime("V1")
    public Page<AuthLog> searchV1(AuthSearchCondition condition, Pageable pageable) {
        return authQueryDslRepositoryV1.search(condition, pageable);
    }

    @Override
    @LogQueryTime("V2")
    public Page<AuthLog> searchV2(AuthSearchCondition condition, Pageable pageable) {
        return authQueryDslRepositoryV2.search(condition, pageable);
    }

    @Override
    @LogQueryTime("V3")
    public Page<AuthLog> searchV3(AuthSearchCondition condition, Pageable pageable) {
        return authQueryDslRepositoryV2.search(condition, pageable);
    }
}
