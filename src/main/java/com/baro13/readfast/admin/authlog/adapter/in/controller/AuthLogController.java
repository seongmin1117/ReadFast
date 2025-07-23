package com.baro13.readfast.admin.authlog.adapter.in.controller;

import com.baro13.readfast.admin.authlog.adapter.in.controller.dto.AuthSearchCondition;
import com.baro13.readfast.admin.authlog.adapter.in.controller.dto.PageResponse;
import com.baro13.readfast.admin.authlog.application.in.ArchivingService;
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
    private final ArchivingService archivingService;

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
     * 수동 아카이빙 배치 실행
     */
    @GetMapping("/archiving/execute")
    public ResponseEntity<ApiResponse<String>> executeArchivingBatch() {
        log.info("수동 아카이빙 배치 실행 요청");
        
        try {
            var result = archivingService.executeArchivingBatch();
            
            if (result.success()) {
                var message = String.format("배치 실행 성공 - 처리건수: %d, 실행시간: %dms", 
                                           result.processedRecords(), result.durationMillis());
                log.info("수동 아카이빙 배치 성공: {}", message);
                return ResponseEntity.ok(ApiResponse.success(message));
            } else {
                log.error("수동 아카이빙 배치 실패: {}", result.message());
                return ResponseEntity.internalServerError()
                        .body(ApiResponse.error("배치 실행 실패: " + result.message()));
            }
            
        } catch (Exception e) {
            log.error("수동 아카이빙 배치 실행 중 예외 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("배치 실행 실패: " + e.getMessage()));
        }
    }
}