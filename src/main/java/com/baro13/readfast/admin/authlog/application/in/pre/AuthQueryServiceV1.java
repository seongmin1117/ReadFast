package com.baro13.readfast.application.in.authlog.pre;

import com.baro13.readfast.adapter.in.controller.authlog.dto.AuthSearchCondition;
import com.baro13.readfast.domain.model.AuthLog;
import com.baro13.readfast.domain.port.AuthLogDbReader;
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
public class AuthQueryServiceV1 {
    private final AuthLogDbReader authLogDbReader;

    public Page<AuthLog> search(AuthSearchCondition condition) {
        Pageable pageable = getPageable(condition);
        return authLogDbReader.searchV1(condition, pageable);
    }

    private Pageable getPageable(AuthSearchCondition condition) {
        Direction dir = Direction.fromString(condition.getDirection());
        Sort sort = Sort.by(dir, condition.getSortBy());
        return PageRequest.of(condition.getPage(), condition.getSize(), sort);
    }
}
