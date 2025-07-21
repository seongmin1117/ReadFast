package com.baro13.readfast.controller;

import com.baro13.readfast.application.AuthLogService;
import com.baro13.readfast.controller.dto.AuthSearchCondition;
import com.baro13.readfast.controller.dto.PageResponse;
import com.baro13.readfast.domain.AuthLog;
import com.baro13.readfast.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v3/auth")
public class AuthLogController {

    private final AuthLogService authLogService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> search(AuthSearchCondition condition) {
        if (condition == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("검색 조건이 필요합니다"));
        }
        
        // 날짜 범위 검증
        if (condition.getStartDate() != null && condition.getEndDate() != null 
            && condition.getStartDate().isAfter(condition.getEndDate())) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("시작 날짜는 종료 날짜보다 이전이어야 합니다"));
        }
        
        Page<AuthLog> response = authLogService.search(condition);
        PageResponse<AuthLog> data = PageResponse.from(response);
        return ApiResponse.ok(data);
    }
}