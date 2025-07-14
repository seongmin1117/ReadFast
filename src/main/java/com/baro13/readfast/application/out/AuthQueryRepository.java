package com.baro13.readfast.application.out;

import com.baro13.readfast.controller.dto.AuthSearchCondition;
import com.baro13.readfast.domain.AuthLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuthQueryRepository {
    Page<AuthLog> search(AuthSearchCondition condition, Pageable pageable);
}
