package com.baro13.readfast.admin.authlog.domain.port;

import com.baro13.readfast.admin.authlog.adapter.in.controller.dto.AuthSearchCondition;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuthLogDbReader {
    Page<AuthLog> searchV1(AuthSearchCondition condition, Pageable pageable);
    Page<AuthLog> searchV2(AuthSearchCondition condition, Pageable pageable);
    Page<AuthLog> searchV3(AuthSearchCondition condition, Pageable pageable);
    
    /**
     * 지정된 날짜보다 오래된 인증 로그 조회 (아카이빙용)
     */
    List<AuthLog> findOlderThan(LocalDateTime cutoffDate, int batchSize);
}
