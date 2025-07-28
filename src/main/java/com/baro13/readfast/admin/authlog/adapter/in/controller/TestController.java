package com.baro13.readfast.admin.authlog.adapter.in.controller;

import com.baro13.readfast.admin.authlog.adapter.out.db.jpa.AuthLogJpaRepository;
import com.baro13.readfast.admin.authlog.adapter.out.db.jpa.entity.AuthLogEntity;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.github.benmanes.caffeine.cache.Cache;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final AuthLogJpaRepository authLogJpaRepository;
    private final Cache<LocalDate, List<AuthLog>> archiveCaffeineCache;

    private final Random random = new Random();

    private static final String[] DEVICES = {
        "WEB", "TABLET", "MOBILE_ANDROID", "MOBILE_IOS"
    };
    @GetMapping("/login")
    public ResponseEntity<AuthLogEntity> login(){
        // 90% 확률로 SUCCESS, 10%는 FAILURE
        String result = random.nextInt(100) < 90 ? "SUCCESS" : "FAILURE";
        String device = DEVICES[random.nextInt(DEVICES.length)];
        String userId = "user" + (random.nextInt(50000) + 1);

        AuthLogEntity entity = new AuthLogEntity(
            null,
            Instant.now(),
            device,
            userId,
            result,
            "/api/login"
        );

        AuthLogEntity saved = authLogJpaRepository.save(entity);
        return ResponseEntity.ok().body(saved);
    }


    @GetMapping("/cache/dates")
    public List<LocalDate> getCachedDates() {
        return archiveCaffeineCache.asMap().keySet().stream()
            .sorted()
            .collect(Collectors.toList());
    }

    @GetMapping("/size")
    public long getCacheSize() {
        return archiveCaffeineCache.estimatedSize();
    }

}
