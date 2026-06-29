package com.example.customer.agent.rag;

import java.util.Objects;

/**
 * 知识检索结果。
 *
 * @param title 知识标题
 * @param source 来源标识
 * @param tenant 租户标识
 * @param category 知识分类
 * @param content 命中内容
 * @param score 相似度得分
 * @author jiangzhibin
 * @since 2026-06-29 20:15:00
 */
public record KnowledgeSearchResult(
        String title,
        String source,
        String tenant,
        String category,
        String content,
        double score) {

    /**
     * 创建知识检索结果。
     */
    public KnowledgeSearchResult {
        Objects.requireNonNull(title, "knowledge title must not be null");
        Objects.requireNonNull(source, "knowledge source must not be null");
        Objects.requireNonNull(tenant, "knowledge tenant must not be null");
        Objects.requireNonNull(category, "knowledge category must not be null");
        Objects.requireNonNull(content, "knowledge content must not be null");
    }
}
