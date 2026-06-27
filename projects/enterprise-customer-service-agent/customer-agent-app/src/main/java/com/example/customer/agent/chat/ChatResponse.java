package com.example.customer.agent.chat;

import com.example.customer.agent.order.OrderResponse;
import java.util.List;

/**
 * 客服对话响应。
 * <p>
 * 结构化字段服务于 Web 调试台查看路由、风险级别、订单证据和下一步动作。
 *
 * @param traceId trace 标识
 * @param route 路由结果
 * @param riskLevel 工具风险级别
 * @param reply 客服回复
 * @param order 订单证据
 * @param nextActions 下一步动作
 * @author jiangzhibin
 * @since 2026-06-27 09:35:00
 */
public record ChatResponse(
        String traceId,
        String route,
        String riskLevel,
        String reply,
        OrderResponse order,
        List<String> nextActions) {
}
