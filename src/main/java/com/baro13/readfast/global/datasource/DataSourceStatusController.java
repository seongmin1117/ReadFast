package com.baro13.readfast.global.datasource;

import com.baro13.readfast.global.response.ApiResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/datasource")
@RequiredArgsConstructor
public class DataSourceStatusController {

    private final DataSourceHealthChecker healthChecker;
    private final DataSourceMonitor monitor;

    /**
     * 데이터소스 상태 조회
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getDataSourceStatus() {
        Map<String, Object> status = healthChecker.getDataSourceStatus();
        return ApiResponse.success(status);
    }

    /**
     * 데이터소스 실패 상태 조회
     */
    @GetMapping("/failures")
    public ApiResponse<String> getFailureStatus() {
        String failureStatus = monitor.getFailureStatus();
        return ApiResponse.success(failureStatus);
    }

    /**
     * 현재 라우팅 상태 조회
     */
    @GetMapping("/routing")
    public ApiResponse<Map<String, Object>> getRoutingStatus() {
        Map<String, Object> routingStatus = Map.of(
            "masterAvailable", monitor.isMasterAvailable(),
            "slaveAvailable", monitor.isSlaveAvailable(),
            "currentRouting", RoutingDataSourceContext.get() != null ? 
                RoutingDataSourceContext.get() : "none",
            "recommendations", Map.of(
                "readOperations", monitor.isSlaveAvailable() ? "slave" : "master",
                "writeOperations", "master"
            )
        );
        return ApiResponse.success(routingStatus);
    }
}