package com.example.customer.agent.knowledge;

import com.example.customer.agent.rag.KnowledgeSearchResult;
import java.util.Objects;

/**
 * 知识搜索命中响应。
 *
 * @param itemId 知识条目标识
 * @param title 标题
 * @param source 来源
 * @param tenant 租户标识
 * @param category 知识分类
 * @param content 命中内容
 * @param score 相似度得分
 * @author jiangzhibin
 * @since 2026-06-30 17:10:00
 */
public record KnowledgeSearchMatchResponse(
        String itemId,
        String title,
        String source,
        String tenant,
        String category,
        String content,
        double score) {

    /**
     * 创建知识搜索命中响应。
     */
    public KnowledgeSearchMatchResponse {
        Objects.requireNonNull(itemId, "knowledge item id must not be null");
        Objects.requireNonNull(title, "knowledge title must not be null");
        Objects.requireNonNull(source, "knowledge source must not be null");
        Objects.requireNonNull(tenant, "knowledge tenant must not be null");
        Objects.requireNonNull(category, "knowledge category must not be null");
        Objects.requireNonNull(content, "knowledge content must not be null");
    }

    /**
     * 从检索结果创建响应。
     *
     * @param result 检索结果
     * @return 响应
     */
    public static KnowledgeSearchMatchResponse from(KnowledgeSearchResult result) {
        return new KnowledgeSearchMatchResponse(
                result.itemId(),
                result.title(),
                result.source(),
                result.tenant(),
                result.category(),
                result.content(),
                result.score());
    }
}
