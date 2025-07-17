package com.baro13.readfast.global;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse {
    private String dateTime;
    private int internalCode;
    private String internalCodeDescription;
    private Object data;

    public static ResponseEntity<ApiResponse> ok(Object data) {
        ApiResponse response = new ApiResponse(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            InternalStatus.SUCCESS.getInternalCode(),
            InternalStatus.SUCCESS.getDescription(),
            data);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    public static ApiResponse error(String message) {
        return new ApiResponse(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            InternalStatus.ERROR.getInternalCode(),
            message,
            null);
    }

}
