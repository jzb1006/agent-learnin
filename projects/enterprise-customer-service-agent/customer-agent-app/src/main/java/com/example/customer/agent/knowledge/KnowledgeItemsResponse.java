package com.example.customer.agent.knowledge;

import java.util.List;
import java.util.Objects;

/**
 * 知识条目列表响应。
 *
 * @param items 知识条目摘要列表
 * @author jiangzhibin
 * @since 2026-06-30 17:10:00
 */
public record KnowledgeItemsResponse(List<KnowledgeItemSummaryResponse> items) {

    /**
     * 创建知识条目列表响应。
     */
    public KnowledgeItemsResponse {
        Objects.requireNonNull(items, "knowledge items must not be null");
        items = List.copyOf(items);
    }
}
