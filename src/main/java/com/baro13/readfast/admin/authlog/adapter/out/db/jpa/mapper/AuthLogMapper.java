package com.baro13.readfast.admin.authlog.adapter.out.db.jpa.mapper;

import com.baro13.readfast.admin.authlog.adapter.out.db.jpa.entity.AuthLogEntity;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;

public final class AuthLogMapper {

    private AuthLogMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static AuthLog toDomain(AuthLogEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return AuthLog.of(
            entity.getId(),
            entity.getDate(),
            entity.getDevice(),
            entity.getUserId(),
            entity.getResult(),
            entity.getEndpoint());
    }

    public static AuthLogEntity toEntity(AuthLog domain) {
        if (domain == null) {
            return null;
        }
        
        return new AuthLogEntity(
            domain.getId(),
            domain.getDate(),
            domain.getDevice(),
            domain.getUserId(),
            domain.getResult(),
            domain.getEndpoint());
    }
}
