package com.example.customer.agent.chat;

/**
 * 客服对话请求。
 *
 * @param tenantId 租户标识
 * @param message 用户消息
 * @author jiangzhibin
 * @since 2026-06-27 09:35:00
 */
public record ChatRequest(String tenantId, String message) {
}
