package com.baro13.readfast.controller;

import com.baro13.readfast.application.IntegratedAuthQueryService;
import com.baro13.readfast.controller.dto.AuthSearchCondition;
import com.baro13.readfast.controller.dto.PageResponse;
import com.baro13.readfast.domain.AuthLog;
import com.baro13.readfast.global.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v3/auth")
public class AuthQueryControllerV3 {

    private final IntegratedAuthQueryService integratedAuthQueryService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchIntegrated(AuthSearchCondition condition) {
        Page<AuthLog> response = integratedAuthQueryService.searchIntegratedV1(condition);
        PageResponse<AuthLog> data = PageResponse.from(response);
        return ApiResponse.ok(data);
    }
    
    @GetMapping("/search-v2")
    public ResponseEntity<ApiResponse> searchIntegratedV2(AuthSearchCondition condition) {
        Page<AuthLog> response = integratedAuthQueryService.searchIntegratedV2(condition);
        PageResponse<AuthLog> data = PageResponse.from(response);
        return ApiResponse.ok(data);
    }
}