package com.example.customer.agent.intent;

import com.example.customer.domain.trace.ConversationRoute;

/**
 * 意图路由结果。
 * <p>
 * 只表达用户消息被分流到的业务方向和基础证据，不承载工具执行结果。
 *
 * @param route 路由结果
 * @param orderId 用户消息中的订单号，没有则为空
 * @param confidence 确定性 fallback 的置信度
 * @param reason 路由原因，便于 trace 和调试台展示
 * @author jiangzhibin
 * @since 2026-06-27 15:40:00
 */
public record IntentRouteResult(
        ConversationRoute route,
        String orderId,
        double confidence,
        String reason) {
}
