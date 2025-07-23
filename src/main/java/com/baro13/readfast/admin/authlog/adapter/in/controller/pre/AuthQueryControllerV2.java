package com.baro13.readfast.admin.authlog.adapter.in.controller.pre;

import com.baro13.readfast.admin.authlog.adapter.in.controller.dto.AuthSearchCondition;
import com.baro13.readfast.admin.authlog.adapter.in.controller.dto.PageResponse;
import com.baro13.readfast.admin.authlog.application.in.pre.AuthQueryServiceV2;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<ApiResponse<PageResponse<AuthLog>>> search(AuthSearchCondition condition) {
        var response = authQueryServiceV2.search(condition);
        var data = PageResponse.from(response);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
