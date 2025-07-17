package com.baro13.readfast.controller.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
@AllArgsConstructor
public class AuthSearchCondition {
    private Instant startDate;
    private Instant endDate;
    private String device;
    private String userId;
    private String result;
    private String endpoint;

    private Integer page;
    private Integer size;

    private String sortBy;
    private String direction;

    // V2 추가
    private Long cursorId;
    private Instant cursorDate;

    public int getPage() {
        return page == null ? 0 : page;
    }

    public int getSize() {
        return size == null ? 10 : size;
    }

    public String getSortBy() {
        return sortBy == null ? "date" : sortBy;
    }

    public String getDirection() {
        return direction == null ? "desc" : direction;
    }
}
