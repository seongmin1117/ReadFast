package com.baro13.readfast.global.response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.baro13.readfast.global.common.TimeZoneConstants;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * API 응답 래퍼 클래스 (Generic 타입 지원)
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {
    private String dateTime;
    private int internalCode;
    private String internalCodeDescription;
    private T data;

    private static String getCurrentDateTime() {
        return LocalDateTime.now(TimeZoneConstants.APPLICATION_ZONE).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
            getCurrentDateTime(),
            InternalStatus.SUCCESS.getInternalCode(),
            InternalStatus.SUCCESS.getDescription(),
            data);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(
            getCurrentDateTime(),
            InternalStatus.ERROR.getInternalCode(),
            message,
            null);
    }

    // 기존 호환성을 위한 메서드들
    public static ResponseEntity<ApiResponse<Object>> ok(Object data) {
        var response = success(data);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
