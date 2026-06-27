package com.example.customer.agent.chat;

import jakarta.validation.constraints.NotBlank;

/**
 * 客服对话请求。
 *
 * @param tenantId 租户标识
 * @param message 用户消息
 * @author jiangzhibin
 * @since 2026-06-27 09:35:00
 */
public record ChatRequest(
        @NotBlank(message = "tenantId 不能为空") String tenantId,
        @NotBlank(message = "message 不能为空") String message) {
}
