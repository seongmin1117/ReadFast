package com.baro13.readfast.controller;

import com.baro13.readfast.application.AuthQueryService;
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
@RequestMapping("/api/v1/auth")
public class AuthQueryController {

    private final AuthQueryService authQueryService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> search(AuthSearchCondition condition) {
        Page<AuthLog> response = authQueryService.search(condition);
        PageResponse<AuthLog> data = PageResponse.from(response);
        return ApiResponse.ok(data);
    }
}
