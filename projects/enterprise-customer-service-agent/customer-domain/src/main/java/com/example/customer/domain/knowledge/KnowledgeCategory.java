package com.example.customer.domain.knowledge;

/**
 * 知识条目分类。
 * <p>
 * 用于区分 FAQ、政策和产品知识，后续 RAG 检索可按分类过滤。
 *
 * @author jiangzhibin
 * @since 2026-06-27 08:45:00
 */
public enum KnowledgeCategory {

    /**
     * 常见问题。
     */
    FAQ,

    /**
     * 业务政策。
     */
    POLICY,

    /**
     * 产品或课程说明。
     */
    PRODUCT
}
