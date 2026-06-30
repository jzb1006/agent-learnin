package com.example.customer.agent.rag;

import java.util.Objects;

/**
 * 当前运行态已索引的知识条目摘要。
 * <p>
 * 用于管理侧调试列表，描述服务生命周期内的索引状态，不替代持久化知识库表。
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
public record KnowledgeIndexedItem(
        String itemId,
        String tenantId,
        String category,
        String title,
        String source,
        String version,
        int indexedChunks,
        String contentPreview) {

    /**
     * 创建知识索引摘要。
     */
    public KnowledgeIndexedItem {
        Objects.requireNonNull(itemId, "knowledge item id must not be null");
        Objects.requireNonNull(tenantId, "knowledge tenant id must not be null");
        Objects.requireNonNull(category, "knowledge category must not be null");
        Objects.requireNonNull(title, "knowledge title must not be null");
        Objects.requireNonNull(source, "knowledge source must not be null");
        Objects.requireNonNull(version, "knowledge version must not be null");
        Objects.requireNonNull(contentPreview, "knowledge content preview must not be null");
    }
}
