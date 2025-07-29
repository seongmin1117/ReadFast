package com.baro13.readfast.admin.authlog.adapter.in.controller;

import com.baro13.readfast.admin.authlog.adapter.in.controller.dto.LoginRequest;
import com.baro13.readfast.admin.authlog.adapter.out.db.cache.AuthLogCache;
import com.baro13.readfast.admin.authlog.adapter.out.db.cache.AuthLogStats;
import com.baro13.readfast.admin.authlog.adapter.out.db.jpa.AuthLogJpaRepository;
import com.baro13.readfast.admin.authlog.adapter.out.db.jpa.entity.AuthLogEntity;
import com.baro13.readfast.admin.authlog.adapter.out.db.jpa.mapper.AuthLogMapper;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.global.response.ApiResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class TestController {

    private final AuthLogJpaRepository authLogJpaRepository;
    private final AuthLogCache authLogCache;
    private final Random random = new Random();

    private static final String[] DEVICES = {
        "WEB", "TABLET", "MOBILE_ANDROID", "MOBILE_IOS"
    };

    @PostMapping("/login")
    @Transactional
    public ResponseEntity<ApiResponse<AuthLog>> login(@RequestBody LoginRequest request) {
        AuthLogEntity entity = new AuthLogEntity(
            null,
            Instant.now(),
            request.device(),
            request.userId(),
            request.result(),
            "/api/login"
        );

        AuthLogEntity saved = authLogJpaRepository.save(entity);
        AuthLog log = AuthLogMapper.toDomain(saved);
        authLogCache.update(log);

        return ResponseEntity.ok(ApiResponse.success(log));
    }

    @GetMapping("/dashboard/today-stats")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<AuthLogStats>> getTodayStats() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        AuthLogStats stats = authLogCache.getStats(today);
        if (stats == null) {
            stats = new AuthLogStats();
        }
        return ResponseEntity.ok(ApiResponse.success(stats));
    }


}
