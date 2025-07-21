package com.baro13.readfast.global.validation;

import com.baro13.readfast.controller.dto.AuthSearchCondition;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Instant;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, AuthSearchCondition> {
    
    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        // 초기화 로직 없음
    }
    
    @Override
    public boolean isValid(AuthSearchCondition condition, ConstraintValidatorContext context) {
        if (condition == null) {
            return true; // null은 다른 어노테이션에서 처리
        }
        
        Instant startDate = condition.getStartDate();
        Instant endDate = condition.getEndDate();
        
        // 둘 다 null이거나 하나만 null인 경우는 유효
        if (startDate == null || endDate == null) {
            return true;
        }
        
        // 시작 날짜가 종료 날짜보다 이후인 경우 유효하지 않음
        return !startDate.isAfter(endDate);
    }
}