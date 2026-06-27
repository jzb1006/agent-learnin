package com.example.customer.domain.trace;

/**
 * 客服对话路由。
 * <p>
 * 表达用户请求被 Agent 初步分流到的业务方向。
 *
 * @author jiangzhibin
 * @since 2026-06-27 08:45:00
 */
public enum ConversationRoute {

    /**
     * 知识问答。
     */
    KNOWLEDGE_QA,

    /**
     * 订单查询。
     */
    ORDER_LOOKUP,

    /**
     * 退款、取消或改签。
     */
    REFUND_OR_CANCEL,

    /**
     * 人工转接。
     */
    HUMAN_HANDOFF,

    /**
     * 可直接回复的问题。
     */
    DIRECT
}
