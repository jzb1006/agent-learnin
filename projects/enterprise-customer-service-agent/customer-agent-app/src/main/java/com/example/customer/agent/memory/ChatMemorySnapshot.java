package com.example.customer.agent.memory;

/**
 * 客服会话记忆快照。
 *
 * @param conversationId 会话标识
 * @param summary 压缩后的会话摘要
 * @param lastOrderId 最近一次有效订单号
 * @author jiangzhibin
 * @since 2026-07-01 14:20:00
 */
public record ChatMemorySnapshot(String conversationId, String summary, String lastOrderId) {
}
