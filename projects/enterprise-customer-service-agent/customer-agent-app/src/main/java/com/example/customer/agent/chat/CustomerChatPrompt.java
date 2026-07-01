package com.example.customer.agent.chat;

/**
 * 客服对话模型提示词上下文。
 *
 * @param tenantId 租户标识
 * @param message 用户消息
 * @param orderEvidence 订单证据
 * @param memorySummary 会话压缩摘要
 * @author jiangzhibin
 * @since 2026-06-27 10:55:00
 */
public record CustomerChatPrompt(String tenantId, String message, String orderEvidence, String memorySummary) {

    /**
     * 兼容不带记忆摘要的提示词上下文。
     *
     * @param tenantId 租户标识
     * @param message 用户消息
     * @param orderEvidence 订单证据
     */
    public CustomerChatPrompt(String tenantId, String message, String orderEvidence) {
        this(tenantId, message, orderEvidence, "");
    }
}
