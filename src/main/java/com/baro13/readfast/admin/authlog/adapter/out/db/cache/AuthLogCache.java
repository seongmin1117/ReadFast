package com.baro13.readfast.admin.authlog.adapter.out.db.cache;

import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.github.benmanes.caffeine.cache.Cache;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthLogCache {

    @Qualifier("authLogStatsCache")
    private final Cache<LocalDate, AuthLogStats> cache;

    public void update(AuthLog log) {
        LocalDate date = log.getDate().atZone(ZoneId.of("UTC")).toLocalDate();

        AuthLogStats stats = cache.get(date, k -> new AuthLogStats());
        stats.update(log.getUserId(), log.getResult());
    }

    public AuthLogStats getStats(LocalDate date) {
        return cache.getIfPresent(date);
    }
}
