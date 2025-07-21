package com.baro13.readfast.infrastructure.batch.config;

import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableBatchProcessing
@EnableScheduling
@RequiredArgsConstructor
public class BatchConfig {
    
    private final BatchThreadPoolProperties batchProperties;
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(batchProperties.getCorePoolSize());
        executor.setMaxPoolSize(batchProperties.getMaxPoolSize());
        executor.setQueueCapacity(batchProperties.getQueueCapacity());
        executor.setThreadNamePrefix(batchProperties.getThreadNamePrefix());
        executor.setKeepAliveSeconds(batchProperties.getKeepAliveSeconds());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(batchProperties.getAwaitTerminationSeconds());
        executor.initialize();
        return executor;
    }
}