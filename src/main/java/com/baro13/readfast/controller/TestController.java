package com.baro13.readfast.controller;

import com.baro13.readfast.domain.AuthLog;
import com.baro13.readfast.global.ApiResponse;
import com.baro13.readfast.infrastructure.scheduler.DataArchivingScheduler;
import com.baro13.readfast.infrastructure.storage.StorageService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class TestController {

    private final StorageService storageService;
    private final DataArchivingScheduler dataArchivingScheduler;

    @PostMapping("/storage/save")
    public ResponseEntity<ApiResponse> testStorageSave(@RequestParam(defaultValue = "2024-01-01") String date) {
        try {
            LocalDate targetDate = LocalDate.parse(date);
            
            // 테스트 데이터 생성
            List<AuthLog> testData = createTestData(10);
            
            // 스토리지에 저장
            storageService.storeData(testData, targetDate);
            
            log.info("테스트 데이터 저장 완료: {} 일자, {} 개 레코드", targetDate, testData.size());
            
            return ApiResponse.ok("저장 성공: " + targetDate + " 일자에 " + testData.size() + "개 레코드 저장");
        } catch (Exception e) {
            log.error("테스트 데이터 저장 실패", e);
            return ApiResponse.ok("저장 실패: " + e.getMessage());
        }
    }

    @GetMapping("/storage/retrieve")
    public ResponseEntity<ApiResponse> testStorageRetrieve(@RequestParam(defaultValue = "2024-01-01") String date) {
        try {
            LocalDate targetDate = LocalDate.parse(date);
            
            // 스토리지에서 조회
            List<AuthLog> retrievedData = storageService.retrieveData(targetDate);
            
            log.info("테스트 데이터 조회 완료: {} 일자, {} 개 레코드", targetDate, retrievedData.size());
            
            return ApiResponse.ok("조회 성공: " + targetDate + " 일자에서 " + retrievedData.size() + "개 레코드 조회");
        } catch (Exception e) {
            log.error("테스트 데이터 조회 실패", e);
            return ApiResponse.ok("조회 실패: " + e.getMessage());
        }
    }

    @GetMapping("/storage/exists")
    public ResponseEntity<ApiResponse> testStorageExists(@RequestParam(defaultValue = "2024-01-01") String date) {
        try {
            LocalDate targetDate = LocalDate.parse(date);
            
            boolean exists = storageService.dataExists(targetDate);
            
            log.info("테스트 데이터 존재 여부 확인: {} 일자, 존재: {}", targetDate, exists);
            
            return ApiResponse.ok("존재 여부: " + targetDate + " 일자 데이터 " + (exists ? "존재" : "없음"));
        } catch (Exception e) {
            log.error("테스트 데이터 존재 여부 확인 실패", e);
            return ApiResponse.ok("확인 실패: " + e.getMessage());
        }
    }

    @PostMapping("/batch/manual")
    public ResponseEntity<ApiResponse> testManualBatch() {
        try {
            log.info("수동 배치 테스트 시작");
            
            dataArchivingScheduler.executeManualArchiving();
            
            return ApiResponse.ok("수동 배치 실행 완료");
        } catch (Exception e) {
            log.error("수동 배치 실행 실패", e);
            return ApiResponse.ok("배치 실행 실패: " + e.getMessage());
        }
    }

    @PostMapping("/data/create-old")
    public ResponseEntity<ApiResponse> createOldTestData(@RequestParam(defaultValue = "100") int count) {
        try {
            // 95일 전 데이터 생성 (DB 보관 기간 90일을 초과)
            Instant oldDate = Instant.now().minus(95, ChronoUnit.DAYS);
            
            List<AuthLog> oldData = IntStream.range(0, count)
                    .mapToObj(i -> AuthLog.of(
                            (long) (i + 1000),
                            oldDate.plus(i, ChronoUnit.MINUTES),
                            "mobile",
                            "test_user_" + i,
                            "SUCCESS",
                            "/api/test/auth"
                    ))
                    .toList();
            
            // 테스트를 위해 스토리지에 직접 저장
            LocalDate targetDate = oldDate.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            storageService.storeData(oldData, targetDate);
            
            log.info("과거 테스트 데이터 생성 완료: {} 개 레코드", count);
            
            return ApiResponse.ok("과거 테스트 데이터 생성 완료: " + count + "개 레코드 (" + targetDate + ")");
        } catch (Exception e) {
            log.error("과거 테스트 데이터 생성 실패", e);
            return ApiResponse.ok("데이터 생성 실패: " + e.getMessage());
        }
    }

    private List<AuthLog> createTestData(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> AuthLog.of(
                        (long) (i + 1),
                        Instant.now().minus(i, ChronoUnit.MINUTES),
                        i % 2 == 0 ? "mobile" : "web",
                        "test_user_" + i,
                        i % 3 == 0 ? "FAIL" : "SUCCESS",
                        "/api/test/endpoint_" + i
                ))
                .toList();
    }
}