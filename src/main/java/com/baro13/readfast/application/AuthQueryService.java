package com.baro13.readfast.application;

import com.baro13.readfast.application.out.AuthQueryRepository;
import com.baro13.readfast.controller.dto.AuthSearchCondition;
import com.baro13.readfast.domain.AuthLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthQueryService {
    private final AuthQueryRepository authQueryRepository;

    public Page<AuthLog> searchV1(AuthSearchCondition condition) {
        Pageable pageable = getPageable(condition);
        return authQueryRepository.searchV1(condition, pageable);
    }

    public Page<AuthLog> searchV2(AuthSearchCondition condition) {
        Pageable pageable = getPageable(condition);
        return authQueryRepository.searchV2(condition,pageable);
    }

    private Pageable getPageable(AuthSearchCondition condition) {
        Direction dir = Direction.fromString(condition.getDirection());
        Sort sort = Sort.by(dir, condition.getSortBy());
        return PageRequest.of(condition.getPage(), condition.getSize(), sort);
    }
}
