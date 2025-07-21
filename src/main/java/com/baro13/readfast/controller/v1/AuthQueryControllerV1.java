package com.baro13.readfast.controller.v1;

import com.baro13.readfast.application.v1.AuthQueryServiceV1;
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
@RequestMapping("/api/v1/auth")
public class AuthQueryControllerV1 {

    private final AuthQueryServiceV1 authQueryServiceV1;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> search(AuthSearchCondition condition) {
        Page<AuthLog> response = authQueryServiceV1.search(condition);
        PageResponse<AuthLog> data = PageResponse.from(response);
        return ApiResponse.ok(data);
    }
}
