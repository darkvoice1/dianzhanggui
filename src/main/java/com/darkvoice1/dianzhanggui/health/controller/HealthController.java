package com.darkvoice1.dianzhanggui.health.controller;

import com.darkvoice1.dianzhanggui.common.ApiResponse;
import com.darkvoice1.dianzhanggui.health.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供应用健康检查接口。 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthService healthService;

    /** 创建健康检查控制器并注入健康服务。 */
    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    /** 返回当前应用的健康状态。 */
    @GetMapping
    public ApiResponse<HealthService.HealthStatus> health() {
        // 调用业务服务获取状态，再包装成统一响应。
        return ApiResponse.success(healthService.getStatus());
    }
}
