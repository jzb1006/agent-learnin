package com.example.customer.domain.approval;

/**
 * 审批动作。
 * <p>
 * 表达客服 Agent 可能触发但需要人工确认的业务动作。
 *
 * @author jiangzhibin
 * @since 2026-06-27 08:45:00
 */
public enum ApprovalAction {

    /**
     * 订单查询，通常不需要审批。
     */
    ORDER_LOOKUP,

    /**
     * 退款订单。
     */
    REFUND_ORDER,

    /**
     * 取消订单。
     */
    CANCEL_ORDER,

    /**
     * 改签订单。
     */
    RESCHEDULE_ORDER
}
