package com.baro13.readfast.admin.authlog.adapter.in.controller;

import com.baro13.readfast.admin.authlog.application.in.ArchivingService;
import com.baro13.readfast.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v3/auth")
public class ArchivingController {

    private final ArchivingService archivingService;

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
