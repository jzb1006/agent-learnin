package com.example.customer.domain.knowledge;

/**
 * 知识条目状态。
 * <p>
 * 只有启用状态的知识条目才能进入检索结果。
 *
 * @author jiangzhibin
 * @since 2026-06-27 08:45:00
 */
public enum KnowledgeStatus {

    /**
     * 可检索。
     */
    ENABLED,

    /**
     * 不可检索。
     */
    DISABLED
}
