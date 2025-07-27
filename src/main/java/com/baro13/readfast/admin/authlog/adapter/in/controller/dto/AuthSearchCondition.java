package com.baro13.readfast.admin.authlog.adapter.in.controller.dto;

import com.baro13.readfast.global.validation.ValidDateRange;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ValidDateRange
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

    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
    private Integer page;
    
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 1000, message = "페이지 크기는 1000 이하여야 합니다")
    private Integer size;

    @Pattern(regexp = "^(id|date|device|userId|result|endpoint)$", 
             message = "정렬 필드는 id, date, device, userId, result, endpoint 중 하나여야 합니다")
    private String sortBy;
    
    @Pattern(regexp = "^(?i)(asc|desc)$",
             message = "정렬 방향은 asc 또는 desc여야 합니다")
    private String direction;

    @Min(value = 1, message = "커서 ID는 1 이상이어야 합니다")
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
