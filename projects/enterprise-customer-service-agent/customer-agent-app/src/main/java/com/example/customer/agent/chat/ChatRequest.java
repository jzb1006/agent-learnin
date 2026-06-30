package com.example.customer.agent.chat;

import jakarta.validation.constraints.NotBlank;

/**
 * 客服对话请求。
 *
 * @param tenantId 兼容旧调试台的租户标识，运行时以 `X-Tenant-ID` 请求头为准
 * @param message 用户消息
 * @author jiangzhibin
 * @since 2026-06-27 09:35:00
 */
public record ChatRequest(
        String tenantId,
        @NotBlank(message = "message 不能为空") String message) {
}
