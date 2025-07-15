package com.baro13.readfast.global.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Order(1)
@Component
public class LoggingAspect {

    @Around("@annotation(logQueryTime)")
    public Object logQueryExecutionTime(ProceedingJoinPoint pjp, LogQueryTime logQueryTime) throws Throwable {
        String version = logQueryTime.value();

        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long duration = System.currentTimeMillis() - start;

        log.info("[{} - 조회 시간 측정] {}ms",
            version,
            duration
        );

        return result;
    }
}
