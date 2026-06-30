package com.example.customer.agent.knowledge;

/**
 * 知识条目删除响应。
 *
 * @param itemId 知识条目标识
 * @param tenantId 租户标识
 * @param deleted 是否已提交删除
 * @author jiangzhibin
 * @since 2026-06-30 16:30:00
 */
public record KnowledgeDeleteResponse(String itemId, String tenantId, boolean deleted) {
}
