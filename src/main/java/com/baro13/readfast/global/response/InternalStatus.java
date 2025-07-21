package com.baro13.readfast.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InternalStatus {

    SUCCESS(0),
    FAIL(500),
    ERROR(500),
    ;
    private final int internalCode;

    public String getDescription() {
        return this.name();
    }
}
