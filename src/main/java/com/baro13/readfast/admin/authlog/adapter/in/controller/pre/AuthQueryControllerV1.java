package com.baro13.readfast.adapter.in.controller.authlog.pre;

import com.baro13.readfast.adapter.in.controller.authlog.dto.AuthSearchCondition;
import com.baro13.readfast.adapter.in.controller.authlog.dto.PageResponse;
import com.baro13.readfast.application.in.authlog.pre.AuthQueryServiceV1;
import com.baro13.readfast.domain.model.AuthLog;
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
