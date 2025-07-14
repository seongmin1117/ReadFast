package com.baro13.readfast.infrastructure;

import com.baro13.readfast.application.out.AuthQueryRepository;
import com.baro13.readfast.controller.dto.AuthSearchCondition;
import com.baro13.readfast.domain.AuthLog;
import com.baro13.readfast.infrastructure.querydsl.AuthQueryDslRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuthQueryRepositoryImpl implements AuthQueryRepository {
    private final AuthQueryDslRepository authQueryDslRepository;
    @Override
    public Page<AuthLog> search(AuthSearchCondition condition, Pageable pageable) {
        return authQueryDslRepository.search(condition, pageable);
    }
}
