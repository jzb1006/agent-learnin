package com.example.customer.agent.api;

import com.example.customer.agent.health.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 本地健康检查 API。
 * <p>
 * 当前用于 Day 04 调试台联通性验证，后续运行态健康检查会交给 Actuator 扩展。
 *
 * @author jiangzhibin
 * @since 2026-06-27 09:35:00
 */
@RestController
public class HealthController {

    /**
     * 返回应用基础健康状态。
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", "customer-agent-app");
    }
}
