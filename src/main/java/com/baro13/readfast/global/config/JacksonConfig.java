package com.baro13.readfast.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson 직렬화 설정
 * 전체 시스템에서 일관된 ISO-8601 UTC 날짜 형식 사용
 */
@Configuration
public class JacksonConfig {

    /**
     * ObjectMapper 설정: ISO-8601 UTC 형식으로 통일
     * Frontend ↔ Backend 간 일관된 날짜 형식 보장
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        var mapper = new ObjectMapper();
        
        // JavaTimeModule 등록 (java.time.* 타입 지원)
        var javaTimeModule = new JavaTimeModule();
        
        // Instant를 ISO-8601 UTC 형식으로 직렬화
        // 예: "2024-01-15T10:30:00.123Z"
        javaTimeModule.addSerializer(java.time.Instant.class, InstantSerializer.INSTANCE);
        
        mapper.registerModule(javaTimeModule);
        
        // TIMESTAMP를 숫자가 아닌 ISO-8601 문자열로 직렬화
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // null 값 처리 설정
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        return mapper;
    }
}