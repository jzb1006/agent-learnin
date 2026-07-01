package com.example.customer.agent.chat;

import jakarta.validation.constraints.NotBlank;

/**
 * 客服对话请求。
 *
 * @param tenantId 兼容旧调试台的租户标识，运行时以 `X-Tenant-ID` 请求头为准
 * @param message 用户消息
 * @param conversationId 会话标识；缺失时服务端生成一次性会话
 * @author jiangzhibin
 * @since 2026-06-27 09:35:00
 */
public record ChatRequest(
        String tenantId,
        @NotBlank(message = "message 不能为空") String message,
        String conversationId) {

    /**
     * 兼容旧调试台和旧测试的双字段构造器。
     *
     * @param tenantId 租户标识
     * @param message 用户消息
     */
    public ChatRequest(String tenantId, String message) {
        this(tenantId, message, null);
    }
}
