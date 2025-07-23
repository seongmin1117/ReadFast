package com.baro13.readfast.admin.authlog.adapter.out.db;

import com.baro13.readfast.admin.authlog.adapter.in.controller.dto.AuthSearchCondition;
import com.baro13.readfast.admin.authlog.adapter.out.db.querydsl.AuthQueryDslRepositoryV1;
import com.baro13.readfast.admin.authlog.adapter.out.db.querydsl.AuthQueryDslRepositoryV2;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.admin.authlog.domain.port.AuthLogDbReader;
import com.baro13.readfast.global.logging.LogQueryTime;
import java.time.LocalDateTime;
import java.util.List;
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

    @Override
    @LogQueryTime("Archive")
    public List<AuthLog> findOlderThan(LocalDateTime cutoffDate, int batchSize) {
        return authQueryDslRepositoryV1.findOlderThan(cutoffDate, batchSize);
    }
}
