package com.baro13.readfast.infrastructure.batch.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "batch.thread-pool")
public class BatchThreadPoolProperties {
    
    private int corePoolSize = 4;
    private int maxPoolSize = 8;
    private int queueCapacity = 100;
    private String threadNamePrefix = "batch-";
    private int keepAliveSeconds = 60;
    private int awaitTerminationSeconds = 30;
}