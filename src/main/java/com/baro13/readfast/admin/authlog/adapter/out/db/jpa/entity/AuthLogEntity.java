package com.baro13.readfast.adapter.out.authlog.db.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "auth_log_entity", indexes = {
    @Index(name = "idx_auth_log_date", columnList = "date"),
    @Index(name = "idx_auth_log_user_date", columnList = "userId, date"),
    @Index(name = "idx_auth_log_result_date", columnList = "result, date"),
    @Index(name = "idx_auth_log_device_date", columnList = "device, date"),
    @Index(name = "idx_auth_log_endpoint_date", columnList = "endpoint, date")
})
@NoArgsConstructor
@AllArgsConstructor
public class AuthLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Instant date;
    private String device;
    private String userId;
    private String result;
    private String endpoint;
}
