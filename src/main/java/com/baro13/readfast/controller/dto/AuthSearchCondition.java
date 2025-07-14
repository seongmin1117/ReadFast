package com.baro13.readfast.controller.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class AuthSearchCondition {
    private Instant startDate;
    private Instant endDate;
    private String device;
    private String userId;
    private String result;
    private String endpoint;

    private Integer page = 0;
    private Integer size = 10;

    private String sortBy = "date";
    private String direction = "desc";
}
