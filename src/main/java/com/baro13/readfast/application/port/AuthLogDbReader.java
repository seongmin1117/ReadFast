package com.baro13.readfast.application.port;

import com.baro13.readfast.controller.dto.AuthSearchCondition;
import com.baro13.readfast.domain.AuthLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuthLogDbReader {
    Page<AuthLog> searchV1(AuthSearchCondition condition, Pageable pageable);
    Page<AuthLog> searchV2(AuthSearchCondition condition, Pageable pageable);
    Page<AuthLog> search(AuthSearchCondition condition, Pageable pageable);

}
