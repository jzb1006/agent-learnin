package com.example.customer.agent.rag;

/**
 * 知识索引写入结果。
 *
 * @param itemId 知识条目标识
 * @param tenantId 租户标识
 * @param indexedChunks 本次写入 chunk 数
 * @param skipped 是否因内容未变化跳过
 * @author jiangzhibin
 * @since 2026-06-30 16:20:00
 */
public record KnowledgeIndexResult(String itemId, String tenantId, int indexedChunks, boolean skipped) {
}
