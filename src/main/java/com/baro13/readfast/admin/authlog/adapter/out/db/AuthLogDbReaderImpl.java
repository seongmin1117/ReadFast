package com.baro13.readfast.admin.authlog.adapter.out.db;

import com.baro13.readfast.admin.authlog.adapter.in.controller.dto.AuthSearchCondition;
import com.baro13.readfast.admin.authlog.adapter.out.db.querydsl.AuthArchiveRepository;
import com.baro13.readfast.admin.authlog.adapter.out.db.querydsl.AuthQueryDslRepository;
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
    private final AuthArchiveRepository archiveRepository; // 아카이빙 전용
    private final AuthQueryDslRepository authQueryRepository; // 메인 검색용

    @Override
    @LogQueryTime("AuthSearch")
    public Page<AuthLog> search(AuthSearchCondition condition, Pageable pageable) {
        return authQueryRepository.search(condition, pageable);
    }

    @Override
    @LogQueryTime("ArchiveCursor")
    public List<AuthLog> findOlderThan(LocalDateTime cutoffDate, int batchSize, Long lastProcessedId) {
        return archiveRepository.findOlderThan(cutoffDate, batchSize, lastProcessedId);
    }
    
    @Override
    @LogQueryTime("ArchiveSnapshot")
    public List<AuthLog> findOlderThanWithSnapshot(LocalDateTime cutoffDate, int limit, Long lastProcessedId) {
        // 스냅샷 격리 수준에서 조회 (MySQL의 경우 REPEATABLE READ)
        return archiveRepository.findOlderThanWithSnapshot(cutoffDate, limit, lastProcessedId);
    }
    
    @Override
    @LogQueryTime("ArchiveDelete")
    public int deleteByIds(List<Long> ids) {
        return archiveRepository.deleteByIds(ids);
    }
    
    @Override
    @LogQueryTime("ArchiveCount")
    public long countOlderThan(LocalDateTime cutoffDate) {
        return archiveRepository.countOlderThan(cutoffDate);
    }
}
