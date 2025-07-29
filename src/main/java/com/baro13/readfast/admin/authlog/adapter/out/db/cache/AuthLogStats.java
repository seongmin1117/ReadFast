package com.baro13.readfast.admin.authlog.adapter.out.db.cache;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

@Getter
public class AuthLogStats {
    private int totalCount = 0;
    private int successCount = 0;
    private final Set<String> uniqueUsers = new HashSet<>();

    public void update(String userId, String result) {
        totalCount++;
        if ("SUCCESS".equalsIgnoreCase(result)) {
            successCount++;
        }
        if (userId != null) {
            uniqueUsers.add(userId);
        }
    }

    public double getSuccessRate() {
        if (totalCount == 0) return 0.0;
        return (successCount * 100.0) / totalCount;
    }

    public int getUniqueUserCount() {
        return uniqueUsers.size();
    }
}
