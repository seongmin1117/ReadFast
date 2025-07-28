package com.baro13.readfast.global.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 전체 시스템에서 사용되는 날짜/시간 유틸리티 클래스
 * ISO-8601 UTC 형식으로 통일된 날짜 처리 보장
 */
public final class DateTimeUtils {
    
    /**
     * ISO-8601 UTC 형식 포매터
     * 예: "2024-01-15T10:30:00.123Z"
     */
    public static final DateTimeFormatter ISO_UTC_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    
    /**
     * 날짜만 표시하는 포매터 (yyyy-MM-dd)
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private DateTimeUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Instant를 ISO-8601 UTC 문자열로 변환
     * Frontend ↔ Backend ↔ Archive 간 일관된 형식 보장
     * 
     * @param instant 변환할 Instant
     * @return ISO-8601 UTC 문자열 (예: "2024-01-15T10:30:00.123Z")
     */
    public static String toIsoUtcString(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.toString(); // Instant.toString()은 자동으로 ISO-8601 UTC 형식
    }
    
    /**
     * ISO-8601 UTC 문자열을 Instant로 파싱
     * 
     * @param isoUtcString ISO-8601 UTC 문자열
     * @return 파싱된 Instant
     * @throws IllegalArgumentException 유효하지 않은 형식인 경우
     */
    public static Instant parseIsoUtcString(String isoUtcString) {
        if (isoUtcString == null || isoUtcString.trim().isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(isoUtcString);
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 ISO-8601 UTC 형식: " + isoUtcString, e);
        }
    }
    
    /**
     * LocalDate를 해당 날짜의 시작 시간(00:00:00)으로 변환
     * 
     * @param date 변환할 날짜
     * @return 해당 날짜의 시작 시간 (UTC)
     */
    public static Instant atStartOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay(TimeZoneConstants.APPLICATION_ZONE).toInstant();
    }
    
    /**
     * LocalDate를 해당 날짜의 끝 시간(23:59:59.999)으로 변환
     * 
     * @param date 변환할 날짜
     * @return 해당 날짜의 끝 시간 (UTC)
     */
    public static Instant atEndOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atTime(23, 59, 59, 999_000_000)
                .atZone(TimeZoneConstants.APPLICATION_ZONE)
                .toInstant();
    }
    
    /**
     * Instant를 애플리케이션 타임존의 LocalDate로 변환
     * 
     * @param instant 변환할 Instant
     * @return 애플리케이션 타임존 기준 LocalDate
     */
    public static LocalDate toLocalDate(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(TimeZoneConstants.APPLICATION_ZONE).toLocalDate();
    }
    
    /**
     * 현재 시간을 ISO-8601 UTC 문자열로 반환
     * 
     * @return 현재 시간의 ISO-8601 UTC 문자열
     */
    public static String nowAsIsoUtcString() {
        return toIsoUtcString(Instant.now());
    }
}