package com.baro13.readfast.global.datasource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Aspect
@Order(0) // 트랜잭션보다 먼저 실행되도록 설정
@Component
@RequiredArgsConstructor
public class DataSourceRoutingAspect {

    private final DataSourceMonitor dataSourceMonitor;

    @Around("@annotation(transactional)")
    public Object routeDataSource(ProceedingJoinPoint joinPoint, Transactional transactional) throws Throwable {
        try {
            if (transactional.readOnly()) {
                // Slave가 사용 가능한지 확인 후 라우팅
                if (dataSourceMonitor.isSlaveAvailable()) {
                    RoutingDataSourceContext.set("slave");
                    log.debug("Routing to SLAVE (read-only operation)");
                } else {
                    RoutingDataSourceContext.set("master");
                    log.warn("Slave unavailable, routing read-only operation to MASTER");
                }
            } else {
                RoutingDataSourceContext.set("master");
                log.debug("Routing to MASTER (write operation)");
            }

            return joinPoint.proceed();
        } finally {
            RoutingDataSourceContext.clear();
        }
    }
}
