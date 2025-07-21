package com.baro13.readfast.controller.v2;

import com.baro13.readfast.application.v2.AuthQueryServiceV2;
import com.baro13.readfast.controller.dto.AuthSearchCondition;
import com.baro13.readfast.controller.dto.PageResponse;
import com.baro13.readfast.domain.AuthLog;
import com.baro13.readfast.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/auth")
public class AuthQueryControllerV2 {

    private final AuthQueryServiceV2 authQueryServiceV2;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> search(AuthSearchCondition condition) {
        Page<AuthLog> response = authQueryServiceV2.search(condition);
        PageResponse<AuthLog> data = PageResponse.from(response);
        return ApiResponse.ok(data);
    }
}
