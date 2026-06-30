package com.example.customer.agent.knowledge;

import com.example.customer.agent.rag.KnowledgeIndexedItem;
import java.util.Objects;

/**
 * 知识条目管理摘要响应。
 *
 * @param itemId 知识条目标识
 * @param tenantId 租户标识
 * @param category 知识分类
 * @param title 标题
 * @param source 来源
 * @param version 版本
 * @param indexedChunks 已索引 chunk 数
 * @param contentPreview 内容预览
 * @author jiangzhibin
 * @since 2026-06-30 17:10:00
 */
public record KnowledgeItemSummaryResponse(
        String itemId,
        String tenantId,
        String category,
        String title,
        String source,
        String version,
        int indexedChunks,
        String contentPreview) {

    /**
     * 创建知识条目摘要响应。
     */
    public KnowledgeItemSummaryResponse {
        Objects.requireNonNull(itemId, "knowledge item id must not be null");
        Objects.requireNonNull(tenantId, "knowledge tenant id must not be null");
        Objects.requireNonNull(category, "knowledge category must not be null");
        Objects.requireNonNull(title, "knowledge title must not be null");
        Objects.requireNonNull(source, "knowledge source must not be null");
        Objects.requireNonNull(version, "knowledge version must not be null");
        Objects.requireNonNull(contentPreview, "knowledge content preview must not be null");
    }

    /**
     * 从索引摘要创建响应。
     *
     * @param item 索引摘要
     * @return 响应
     */
    public static KnowledgeItemSummaryResponse from(KnowledgeIndexedItem item) {
        return new KnowledgeItemSummaryResponse(
                item.itemId(),
                item.tenantId(),
                item.category(),
                item.title(),
                item.source(),
                item.version(),
                item.indexedChunks(),
                item.contentPreview());
    }
}
