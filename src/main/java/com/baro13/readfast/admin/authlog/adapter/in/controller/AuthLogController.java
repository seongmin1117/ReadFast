package com.baro13.readfast.admin.authlog.adapter.in.controller;

import com.baro13.readfast.admin.authlog.adapter.in.controller.dto.AuthSearchCondition;
import com.baro13.readfast.admin.authlog.adapter.in.controller.dto.PageResponse;
import com.baro13.readfast.admin.authlog.application.in.AuthLogService;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증로그 조회 컨트롤러 (MVP)
 * 인증로그 대시보드의 핵심 기능인 로그 조회/검색 기능 제공
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v3/auth")
public class AuthLogController {

    private final AuthLogService authLogService;


    /**
     * 인증로그 검색 (통합 조회: DB + 스토리지)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<AuthLog>>> search(@Valid AuthSearchCondition condition) {
        try {
            var response = authLogService.search(condition);
            var data = PageResponse.from(response);
            
            log.debug("인증로그 검색 완료 - 조건: {}, 결과: {}건", condition, data.totalElements());
            return ResponseEntity.ok(ApiResponse.success(data));
            
        } catch (Exception e) {
            log.error("인증로그 검색 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("인증로그 검색 실패: " + e.getMessage()));
        }
    }

    /**
     * 인증로그 검색 V2 (통합 조회: DB + 스토리지, 커서 기반)
     */
    @GetMapping("/search-v2")
    public ResponseEntity<ApiResponse<PageResponse<AuthLog>>> searchV2(@Valid AuthSearchCondition condition) {
        try {
            var response = authLogService.searchV2(condition);
            var data = PageResponse.from(response);
            
            log.debug("인증로그 검색 V2 완료 - 조건: {}, 결과: {}건", condition, data.totalElements());
            return ResponseEntity.ok(ApiResponse.success(data));
            
        } catch (Exception e) {
            log.error("인증로그 검색 V2 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("인증로그 검색 V2 실패: " + e.getMessage()));
        }
    }


}