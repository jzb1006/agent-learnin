package com.example.customer.agent.knowledge;

import com.example.customer.agent.rag.KnowledgeReindexResult;

/**
 * 知识库重建索引响应。
 *
 * @param documents 读取文档数
 * @param indexedChunks 本次写入 chunk 数
 * @param skippedItems 跳过未变化条目数
 * @author jiangzhibin
 * @since 2026-06-30 16:30:00
 */
public record KnowledgeReindexResponse(int documents, int indexedChunks, int skippedItems) {

    /**
     * 从重建结果创建响应。
     *
     * @param result 重建结果
     * @return API 响应
     */
    public static KnowledgeReindexResponse from(KnowledgeReindexResult result) {
        return new KnowledgeReindexResponse(result.documents(), result.indexedChunks(), result.skippedItems());
    }
}
