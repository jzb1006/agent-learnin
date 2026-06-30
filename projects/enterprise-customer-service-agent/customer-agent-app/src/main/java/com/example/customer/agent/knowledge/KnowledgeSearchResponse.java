package com.example.customer.agent.knowledge;

import java.util.List;
import java.util.Objects;

/**
 * 知识搜索响应。
 *
 * @param query 查询词
 * @param tenantId 租户标识
 * @param topK 最大召回条数
 * @param matches 命中列表
 * @author jiangzhibin
 * @since 2026-06-30 17:10:00
 */
public record KnowledgeSearchResponse(
        String query,
        String tenantId,
        int topK,
        List<KnowledgeSearchMatchResponse> matches) {

    /**
     * 创建知识搜索响应。
     */
    public KnowledgeSearchResponse {
        Objects.requireNonNull(query, "knowledge search query must not be null");
        Objects.requireNonNull(tenantId, "knowledge tenant id must not be null");
        Objects.requireNonNull(matches, "knowledge search matches must not be null");
        matches = List.copyOf(matches);
    }
}
