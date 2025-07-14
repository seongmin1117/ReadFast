package com.baro13.readfast.infrastructure;

import com.baro13.readfast.domain.AuthLog;
import com.baro13.readfast.infrastructure.jpa.AuthLogEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthLogMapper {

    public static AuthLog toDomain(AuthLogEntity entity) {
        return AuthLog.of(
            entity.getId(),
            entity.getDate(),
            entity.getDevice(),
            entity.getUserId(),
            entity.getResult(),
            entity.getEndpoint());
    }

}
