package com.example.customer.agent.rag;

/**
 * 知识库重建索引结果。
 *
 * @param documents 读取文档数
 * @param indexedChunks 本次写入 chunk 数
 * @param skippedItems 内容未变化跳过条目数
 * @author jiangzhibin
 * @since 2026-06-30 16:20:00
 */
public record KnowledgeReindexResult(int documents, int indexedChunks, int skippedItems) {
}
