package com.example.customer.agent.chat;

import java.util.List;

/**
 * 客服 Agent 结构化响应。
 * <p>
 * 统一承载模型或本地 fallback 生成的客服回复，便于 API、调试台和后续 trace 复用同一契约。
 *
 * @param route 路由结果
 * @param answer 客服回复正文
 * @param sources 回复依据来源
 * @param riskLevel 工具风险级别
 * @param nextActions 下一步动作
 * @param traceId trace 标识
 * @author jiangzhibin
 * @since 2026-06-27 16:05:00
 */
public record CustomerAgentResponse(
        String route,
        String answer,
        List<String> sources,
        String riskLevel,
        List<String> nextActions,
        String traceId) {

    /**
     * 创建客服 Agent 结构化响应。
     */
    public CustomerAgentResponse {
        sources = List.copyOf(sources);
        nextActions = List.copyOf(nextActions);
    }
}
