package com.baro13.readfast.application.v2;

import com.baro13.readfast.application.port.AuthLogDbReader;
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
public class AuthQueryServiceV2 {
    private final AuthLogDbReader authLogDbReader;

    public Page<AuthLog> search(AuthSearchCondition condition) {
        validateSearchCondition(condition);
        Pageable pageable = getPageable(condition);
        return authLogDbReader.searchV2(condition, pageable);
    }

    private Pageable getPageable(AuthSearchCondition condition) {
        Direction dir = parseDirection(condition.getDirection());
        Sort sort = Sort.by(dir, condition.getSortBy());
        return PageRequest.of(condition.getPage(), condition.getSize(), sort);
    }

    private void validateSearchCondition(AuthSearchCondition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Search condition cannot be null");
        }
        if (condition.getPage() < 0 || condition.getSize() <= 0) {
            throw new IllegalArgumentException("Invalid pagination parameters");
        }
        if (condition.getSize() > 1000) {
            throw new IllegalArgumentException("Page size cannot exceed 1000");
        }
    }

    private Direction parseDirection(String direction) {
        try {
            return Direction.fromString(direction);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid sort direction: {}, defaulting to DESC", direction);
            return Direction.DESC;
        }
    }
}
