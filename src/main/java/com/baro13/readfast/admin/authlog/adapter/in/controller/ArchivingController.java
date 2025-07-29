package com.baro13.readfast.admin.authlog.adapter.in.controller;

import com.baro13.readfast.admin.authlog.application.in.ArchivingService;
import com.baro13.readfast.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

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

    /**
     * 날짜 지정 아카이빙 배치 실행
     */
    @PostMapping("/archiving/execute-by-date")
    public ResponseEntity<ApiResponse<String>> executeArchivingBatchByDate(
            @RequestParam String targetDate) {
        log.info("날짜 지정 아카이빙 배치 실행 요청: {}", targetDate);

        try {
            LocalDate date = LocalDate.parse(targetDate);
            var result = archivingService.executeArchivingBatchByDate(date);

            if (result.success()) {
                var message = String.format("날짜 %s 배치 실행 성공 - 처리건수: %d, 실행시간: %dms",
                    targetDate, result.processedRecords(), result.durationMillis());
                log.info("날짜 지정 아카이빙 배치 성공: {}", message);
                return ResponseEntity.ok(ApiResponse.success(message));
            } else {
                log.error("날짜 지정 아카이빙 배치 실패: {}", result.message());
                return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("배치 실행 실패: " + result.message()));
            }

        } catch (Exception e) {
            log.error("날짜 지정 아카이빙 배치 실행 중 예외 발생", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("배치 실행 실패: " + e.getMessage()));
        }
    }

    /**
     * 배치 아카이브 상태 조회
     */
    @GetMapping("/archiving/status")
    public ResponseEntity<ApiResponse<Object>> getArchivingStatus() {
        try {
            var status = archivingService.getArchivingStatus();
            return ResponseEntity.ok(ApiResponse.success(status));
        } catch (Exception e) {
            log.error("배치 상태 조회 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("배치 상태 조회 실패: " + e.getMessage()));
        }
    }
}
