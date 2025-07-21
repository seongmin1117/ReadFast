package com.baro13.readfast.application.port;

public interface RetentionPolicyProvider {
    
    int getDbRetentionDays();
    
    int getTotalRetentionDays();
    
    int getBatchSize();
}