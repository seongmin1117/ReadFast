package com.baro13.readfast.global.common;

import java.time.ZoneId;

/**
 * 애플리케이션 전체에서 사용되는 타임존 상수 정의
 */
public final class TimeZoneConstants {
    
    /**
     * 애플리케이션 기본 타임존 (Asia/Seoul)
     */
    public static final ZoneId APPLICATION_ZONE = ZoneId.of("Asia/Seoul");
    
    private TimeZoneConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}