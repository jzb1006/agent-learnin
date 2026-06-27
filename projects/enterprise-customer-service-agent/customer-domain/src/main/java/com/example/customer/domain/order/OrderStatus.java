package com.example.customer.domain.order;

/**
 * 客户订单状态。
 * <p>
 * 当前只为客服查询和风险判断提供领域枚举，不表达支付或履约系统的完整状态机。
 *
 * @author jiangzhibin
 * @since 2026-06-27 08:45:00
 */
public enum OrderStatus {

    /**
     * 待支付。
     */
    PENDING_PAYMENT,

    /**
     * 已支付。
     */
    PAID,

    /**
     * 已取消。
     */
    CANCELLED,

    /**
     * 已退款。
     */
    REFUNDED
}
