package com.example.customer.agent.health;

/**
 * 健康检查响应。
 *
 * @param status 服务状态
 * @param service 服务名称
 * @author jiangzhibin
 * @since 2026-06-27 09:35:00
 */
public record HealthResponse(String status, String service) {
}
