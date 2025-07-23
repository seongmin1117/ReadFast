package com.baro13.readfast.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 테스트 환경을 위한 설정
 * Spring Boot 3.x Executor Bean 충돌 해결
 */
@TestConfiguration
public class TestConfig {
    
    /**
     * 테스트에서 사용할 Primary Executor Bean
     * applicationTaskExecutor와 taskScheduler 충돌 해결
     */
    @Bean
    @Primary
    public Executor testTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("test-async-");
        executor.initialize();
        return executor;
    }
}