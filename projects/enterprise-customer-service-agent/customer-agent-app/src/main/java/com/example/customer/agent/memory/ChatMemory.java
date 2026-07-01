package com.example.customer.agent.memory;

import com.example.customer.domain.trace.ConversationRoute;

/**
 * 客服短期会话记忆。
 * <p>
 * 只暴露业务语义，具体存储可切换为 Redis 或本地内存。
 *
 * @author jiangzhibin
 * @since 2026-07-01 16:20:00
 */
public interface ChatMemory {

    /**
     * 读取当前会话的记忆快照；请求未传 conversationId 时会生成新会话。
     *
     * @param tenantId 租户标识
     * @param requestedConversationId 请求传入的会话标识
     * @return 会话记忆快照
     */
    ChatMemorySnapshot snapshot(String tenantId, String requestedConversationId);

    /**
     * 记录一轮对话结果，并返回更新后的记忆快照。
     *
     * @param tenantId 租户标识
     * @param conversationId 会话标识
     * @param userMessage 用户消息
     * @param route 路由结果
     * @param orderId 本轮有效订单号
     * @return 更新后的会话记忆快照
     */
    ChatMemorySnapshot remember(
            String tenantId,
            String conversationId,
            String userMessage,
            ConversationRoute route,
            String orderId);
}
