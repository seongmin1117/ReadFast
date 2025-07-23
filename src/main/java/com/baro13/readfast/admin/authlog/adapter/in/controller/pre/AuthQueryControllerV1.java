package com.baro13.readfast.admin.authlog.adapter.in.controller.pre;

import com.baro13.readfast.admin.authlog.adapter.in.controller.dto.AuthSearchCondition;
import com.baro13.readfast.admin.authlog.adapter.in.controller.dto.PageResponse;
import com.baro13.readfast.admin.authlog.application.in.pre.AuthQueryServiceV1;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<ApiResponse<PageResponse<AuthLog>>> search(AuthSearchCondition condition) {
        var response = authQueryServiceV1.search(condition);
        var data = PageResponse.from(response);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
