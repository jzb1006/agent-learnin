package com.example.customer.domain.tenant;

/**
 * 租户状态。
 * <p>
 * 用于区分租户是否允许参与客服 Agent 的订单查询、知识库检索和工具调用。
 *
 * @author jiangzhibin
 * @since 2026-06-27 08:45:00
 */
public enum TenantStatus {

    /**
     * 正常启用。
     */
    ACTIVE,

    /**
     * 停用，不应继续提供业务能力。
     */
    DISABLED
}
