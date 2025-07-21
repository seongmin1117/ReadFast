package com.baro13.readfast.infrastructure.policy;

import com.baro13.readfast.application.port.RetentionPolicyProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RetentionPolicyProviderImpl implements RetentionPolicyProvider {
    
    private final DataRetentionProperties properties;
    
    @Override
    public int getDbRetentionDays() {
        return properties.getDbRetentionDays();
    }
    
    @Override
    public int getTotalRetentionDays() {
        return properties.getTotalRetentionDays();
    }
    
    @Override
    public int getBatchSize() {
        return properties.getBatchSize();
    }
}