package com.example.customer.agent.knowledge;

import com.example.customer.agent.rag.KnowledgeIndexResult;

/**
 * 知识条目索引响应。
 *
 * @param itemId 知识条目标识
 * @param tenantId 租户标识
 * @param indexedChunks 本次写入 chunk 数
 * @param skipped 是否跳过未变化内容
 * @author jiangzhibin
 * @since 2026-06-30 16:30:00
 */
public record KnowledgeItemResponse(String itemId, String tenantId, int indexedChunks, boolean skipped) {

    /**
     * 从索引结果创建响应。
     *
     * @param result 索引结果
     * @return API 响应
     */
    public static KnowledgeItemResponse from(KnowledgeIndexResult result) {
        return new KnowledgeItemResponse(result.itemId(), result.tenantId(), result.indexedChunks(), result.skipped());
    }
}
