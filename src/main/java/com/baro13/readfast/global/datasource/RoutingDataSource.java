package com.baro13.readfast.global.datasource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

@Slf4j
public class RoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        String lookupKey = RoutingDataSourceContext.get(); // "master" or "slave"
        log.info("[📦DB ROUTE] 현재 요청은 '{}' 데이터소스로 라우팅됩니다.", lookupKey);
        return RoutingDataSourceContext.get();
    }
}
