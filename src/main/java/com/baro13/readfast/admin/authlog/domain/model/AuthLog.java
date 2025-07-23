package com.baro13.readfast.admin.authlog.domain.model;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthLog {
    private Long id;
    private Instant date;
    private String device;
    private String userId;
    private String result;
    private String endpoint;

    public static AuthLog of(Long id, Instant date, String device, String userId, String result, String endpoint) {
        return AuthLog.builder()
            .id(id)
            .date(date)
            .device(device)
            .userId(userId)
            .result(result)
            .endpoint(endpoint)
            .build();
    }
}
