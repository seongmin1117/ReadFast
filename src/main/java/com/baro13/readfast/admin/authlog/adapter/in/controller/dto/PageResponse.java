package com.baro13.readfast.admin.authlog.adapter.in.controller.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
    List<T> content,
    int size,
    boolean hasNext,
    boolean isCursorBased
) {
    
    // 커서 기반 페이지네이션용 팩토리 메서드
    public static <T> PageResponse<T> cursorBased(List<T> content, int requestedSize, boolean hasNext) {
        return new PageResponse<>(
            content,
            requestedSize,
            hasNext,
            true
        );
    }
    
    // 기존 Page 객체에서 변환 (레거시 지원)
    public static <T> PageResponse<T> from(Page<T> page) {
        // 모든 페이지네이션을 커서 기반으로 처리
        boolean hasNext = page.getContent().size() == page.getSize() && !page.isEmpty();
        
        return new PageResponse<>(
            page.getContent(),
            page.getSize(),
            hasNext,
            true
        );
    }
}
